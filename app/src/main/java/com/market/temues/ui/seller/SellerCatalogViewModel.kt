package com.market.temues.ui.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EstadoCatalogoUI {
    object Cargando : EstadoCatalogoUI()
    data class Exito(val productos: List<Product>) : EstadoCatalogoUI()
    data class Error(val mensaje: String) : EstadoCatalogoUI()
    object Vacio : EstadoCatalogoUI()
}

@HiltViewModel
class SellerCatalogViewModel @Inject constructor(
    private val fuenteRemotaProducto: ProductRemoteDataSource,
    private val autenticacion: FirebaseAuth
) : ViewModel() {

    private val _estadoUI = MutableStateFlow<EstadoCatalogoUI>(EstadoCatalogoUI.Cargando)
    val estadoUI: StateFlow<EstadoCatalogoUI> = _estadoUI.asStateFlow()

    private var cargarJob: Job? = null

    init {
        cargarProductos()
    }

    private fun cargarProductos() {
        cargarJob?.cancel()
        val idUsuario = autenticacion.currentUser?.uid ?: return
        cargarJob = fuenteRemotaProducto.getBySeller(idUsuario)
            .onEach { listaProductos ->
                if (listaProductos.isEmpty()) {
                    _estadoUI.value = EstadoCatalogoUI.Vacio
                } else {
                    _estadoUI.value = EstadoCatalogoUI.Exito(listaProductos)
                }
            }
            .catch { error ->
                _estadoUI.value = EstadoCatalogoUI.Error(error.message ?: "Error desconocido")
            }
            .launchIn(viewModelScope)
    }

    fun eliminarProducto(idProducto: String) {
        viewModelScope.launch {
            try {
                fuenteRemotaProducto.delete(idProducto)
                cargarProductos()
            } catch (e: Exception) {
                _estadoUI.value = EstadoCatalogoUI.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    fun cambiarEstado(idProducto: String, estadoActual: String) {
        viewModelScope.launch {
            try {
                val nuevoEstado = if (estadoActual == "activo") "vendido" else "activo"
                fuenteRemotaProducto.getById(idProducto).firstOrNull()?.let { producto ->
                    fuenteRemotaProducto.update(producto.copy(status = nuevoEstado))
                }
            } catch (e: Exception) {
                _estadoUI.value = EstadoCatalogoUI.Error(e.message ?: "Error al cambiar estado")
            }
        }
    }
}