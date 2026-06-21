package com.market.temues.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.data.local.dao.CarritoDao
import com.market.temues.data.remote.orden.OrdenRemoteDataSource
import com.market.temues.model.ArticuloOrden
import com.market.temues.model.Orden
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PagoViewModel @Inject constructor(
    private val carritoDao: CarritoDao,
    private val ordenDataSource: OrdenRemoteDataSource,
    private val auth: FirebaseAuth
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

    fun confirmarPedido() {
        val usuario = auth.currentUser ?: run {
            resultadoOrden.value = ResultadoOrden.Error("Debes iniciar sesión para continuar")
            return
        }
        val items = articulos.value
        if (items.isEmpty()) {
            resultadoOrden.value = ResultadoOrden.Error("El carrito está vacío")
            return
        }
        viewModelScope.launch {
            cargando.value = true
            try {
                val codigo = (1000..9999).random().toString()
                val primerArticulo = items.first()
                val orden = Orden(
                    usuarioId = usuario.uid,
                    articulos = items.map {
                        ArticuloOrden(
                            productoId = it.productoId,
                            nombreProducto = it.nombreProducto,
                            precio = it.precio,
                            cantidad = it.cantidad
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
                carritoDao.limpiarTodo()
                resultadoOrden.value = ResultadoOrden.Exitoso(codigo)
            } catch (e: Exception) {
                resultadoOrden.value = ResultadoOrden.Error(e.message ?: "Error al confirmar el pedido")
            } finally {
                cargando.value = false
            }
        }
    }
}
