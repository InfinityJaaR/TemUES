package com.market.temues.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.market.temues.data.local.dao.CarritoDao
import com.market.temues.data.local.entity.CarritoEntidad
import com.market.temues.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarritoViewModel @Inject constructor(
    private val carritoDao: CarritoDao
) : ViewModel() {

    val articulos: StateFlow<List<CarritoEntidad>> = carritoDao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val total: StateFlow<Double> = carritoDao.obtenerTotal()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun agregarAlCarrito(producto: Product, cantidad: Int = 1) {
        viewModelScope.launch {
            val articuloExistente = carritoDao.obtenerArticulo(producto.id)
            val nuevaCantidad = (articuloExistente?.cantidad ?: 0) + cantidad
            val urlImagen = producto.images.firstOrNull() ?: ""
            carritoDao.insertarOActualizar(
                CarritoEntidad(
                    productoId = producto.id,
                    nombreProducto = producto.name,
                    precio = producto.price,
                    urlImagen = urlImagen,
                    cantidad = nuevaCantidad,
                    vendedorId = producto.sellerId,
                    lugarEntrega = producto.location
                )
            )
        }
    }

    fun eliminarDelCarrito(productoId: String) {
        viewModelScope.launch { carritoDao.eliminar(productoId) }
    }

    fun aumentarCantidad(productoId: String) {
        viewModelScope.launch {
            val articulo = carritoDao.obtenerArticulo(productoId) ?: return@launch
            carritoDao.insertarOActualizar(articulo.copy(cantidad = articulo.cantidad + 1))
        }
    }

    fun disminuirCantidad(productoId: String) {
        viewModelScope.launch {
            val articulo = carritoDao.obtenerArticulo(productoId) ?: return@launch
            if (articulo.cantidad > 1) {
                carritoDao.insertarOActualizar(articulo.copy(cantidad = articulo.cantidad - 1))
            } else {
                carritoDao.eliminar(productoId)
            }
        }
    }

    fun limpiarCarrito() {
        viewModelScope.launch { carritoDao.limpiarTodo() }
    }
}
