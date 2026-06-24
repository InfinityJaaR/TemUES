package com.market.temues.ui.checkout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.BuildConfig
import com.market.temues.data.local.dao.CarritoDao
import com.market.temues.data.remote.orden.OrdenRemoteDataSource
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.model.ArticuloOrden
import com.market.temues.model.Orden
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class PagoViewModel @Inject constructor(
    private val carritoDao: CarritoDao,
    private val ordenDataSource: OrdenRemoteDataSource,
    private val fuenteRemotaProducto: ProductRemoteDataSource,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val articulos = carritoDao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val total: StateFlow<Double> = carritoDao.obtenerTotal()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val metodoPago = MutableStateFlow("efectivo")

    val cargando = MutableStateFlow(false)

    val resultadoOrden = MutableStateFlow<ResultadoOrden?>(null)

    fun resetResultado() { resultadoOrden.value = null }

    sealed class ResultadoOrden {
        data class Exitoso(val codigo: String) : ResultadoOrden()
        data class Error(val mensaje: String) : ResultadoOrden()
    }

    private suspend fun ejecutarConfirmacion(): String {
        val usuario = auth.currentUser
            ?: throw Exception("Debes iniciar sesión para continuar")
        val items = articulos.value
        if (items.isEmpty()) throw Exception("El carrito está vacío")
        val codigo = (1000..9999).random().toString()
        val primerArticulo = items.first()
        val orden = Orden(
            usuarioId = usuario.uid,
            articulos = items.map {
                ArticuloOrden(
                    productoId = it.productoId,
                    nombreProducto = it.nombreProducto,
                    precio = it.precio,
                    cantidad = it.cantidad,
                    urlImagen = it.urlImagen
                )
            },
            total = total.value,
            metodoPago = metodoPago.value,
            estado = "pendiente",
            codigo = codigo,
            vendedorId = primerArticulo.vendedorId,
            lugarEntrega = primerArticulo.lugarEntrega
        )
        ordenDataSource.crear(orden)

        val nombreComprador = usuario.displayName ?: "Un comprador"
        firestore.collection("users").document(orden.vendedorId)
            .collection("notifications").add(
                mapOf(
                    "type" to "order_placed",
                    "orderId" to codigo,
                    "texto" to "Nueva orden #$codigo de $nombreComprador"
                )
            )

        for (item in items) {
            val producto = fuenteRemotaProducto.getById(item.productoId).first() ?: continue
            val camposActualizados = if (producto.hasStock) {
                val nuevoStock = (producto.stock - item.cantidad).coerceAtLeast(0)
                mapOf(
                    "stock" to nuevoStock,
                    "status" to if (nuevoStock <= 0) "vendido" else producto.status,
                    "updatedAt" to System.currentTimeMillis()
                )
            } else {
                mapOf("status" to "vendido", "updatedAt" to System.currentTimeMillis())
            }
            firestore.collection("products").document(item.productoId)
                .update(camposActualizados).await()
        }

        carritoDao.limpiarTodo()
        return codigo
    }

    fun confirmarPedido() {
        viewModelScope.launch {
            cargando.value = true
            try {
                val codigo = ejecutarConfirmacion()
                resultadoOrden.value = ResultadoOrden.Exitoso(codigo)
            } catch (e: Exception) {
                resultadoOrden.value = ResultadoOrden.Error(e.message ?: "Error al confirmar el pedido")
            } finally {
                cargando.value = false
            }
        }
    }

    fun procesarPagoTarjeta(params: PaymentMethodCreateParams) {
        viewModelScope.launch {
            cargando.value = true
            try {
                val stripe = Stripe(context, BuildConfig.STRIPE_KEY)
                suspendCancellableCoroutine { cont ->
                    stripe.createPaymentMethod(
                        paymentMethodCreateParams = params,
                        callback = object : ApiResultCallback<PaymentMethod> {
                            override fun onSuccess(result: PaymentMethod) = cont.resume(result)
                            override fun onError(e: Exception) = cont.resumeWithException(e)
                        }
                    )
                }
                val codigo = ejecutarConfirmacion()
                resultadoOrden.value = ResultadoOrden.Exitoso(codigo)
            } catch (e: Exception) {
                resultadoOrden.value = ResultadoOrden.Error(e.message ?: "Datos de tarjeta inválidos")
            } finally {
                cargando.value = false
            }
        }
    }
}
