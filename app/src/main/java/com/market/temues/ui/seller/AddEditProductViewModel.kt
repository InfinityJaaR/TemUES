package com.market.temues.ui.seller

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.data.remote.category.CategoryRemoteDataSource
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.data.remote.storage.StorageDataSource
import com.market.temues.model.Category
import com.market.temues.model.Product
import com.market.temues.utils.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EstadoGuardado {
    object Inactivo : EstadoGuardado()
    object Guardando : EstadoGuardado()
    object Exito : EstadoGuardado()
    data class Error(val mensaje: String) : EstadoGuardado()
}

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val fuenteProducto: ProductRemoteDataSource,
    private val fuenteCategoria: CategoryRemoteDataSource,
    private val fuenteAlmacenamiento: StorageDataSource,
    private val autenticacion: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val idProducto: String? = savedStateHandle["productId"]

    private val _categorias = MutableStateFlow<List<Category>>(emptyList())
    val categorias: StateFlow<List<Category>> = _categorias.asStateFlow()

    val nombre = MutableStateFlow("")
    val descripcion = MutableStateFlow("")
    val precio = MutableStateFlow("")
    val condicion = MutableStateFlow("nuevo")
    val idCategoria = MutableStateFlow("")
    val ubicacion = MutableStateFlow("")
    val etiquetas = MutableStateFlow<List<String>>(emptyList())
    val imagenes = MutableStateFlow<List<String>>(emptyList())
    val tieneStock = MutableStateFlow(false)
    val cantidadStock = MutableStateFlow("")

    private val _estadoGuardado = MutableStateFlow<EstadoGuardado>(EstadoGuardado.Inactivo)
    val estadoGuardado: StateFlow<EstadoGuardado> = _estadoGuardado.asStateFlow()

    init {
        cargarCategorias()
        if (!idProducto.isNullOrEmpty()) {
            cargarProducto(idProducto)
        }
    }

    private fun cargarCategorias() {
        fuenteCategoria.getAll()
            .onEach { _categorias.value = it }
            .launchIn(viewModelScope)
    }

    private fun cargarProducto(id: String) {
        fuenteProducto.getById(id)
            .onEach { producto ->
                producto?.let {
                    nombre.value = it.name
                    descripcion.value = it.description
                    precio.value = it.price.toString()
                    condicion.value = it.condition
                    idCategoria.value = it.categoryId
                    ubicacion.value = it.location
                    etiquetas.value = it.tags
                    imagenes.value = it.images
                    tieneStock.value = it.hasStock
                    cantidadStock.value = if (it.hasStock) it.stock.toString() else ""
                }
            }
            .launchIn(viewModelScope)
    }

    fun subirImagen(uri: Uri) {
        fuenteAlmacenamiento.uploadProductImage(uri)
            .onEach { resultado ->
                resultado.onSuccess { ruta ->
                    fuenteAlmacenamiento.getImageUrl(ruta).onEach { resultadoURL ->
                        resultadoURL.onSuccess { url ->
                            imagenes.value = imagenes.value + url
                        }
                    }.launchIn(viewModelScope)
                }
            }
            .launchIn(viewModelScope)
    }

    fun eliminarImagen(indice: Int) {
        val imagenesActuales = imagenes.value.toMutableList()
        if (indice in imagenesActuales.indices) {
            imagenesActuales.removeAt(indice)
            imagenes.value = imagenesActuales
        }
    }

    fun guardar() {
        if (!ValidationUtils.isNotEmpty(nombre.value, precio.value, idCategoria.value)) {
            _estadoGuardado.value = EstadoGuardado.Error("Nombre, precio y categoría son obligatorios")
            return
        }
        
        if (!ValidationUtils.isValidPrice(precio.value)) {
            _estadoGuardado.value = EstadoGuardado.Error("El precio debe ser mayor a 0")
            return
        }

        if (tieneStock.value && (cantidadStock.value.toIntOrNull() ?: 0) <= 0) {
            _estadoGuardado.value = EstadoGuardado.Error("Ingresa una cantidad de stock válida")
            return
        }

        viewModelScope.launch {
            _estadoGuardado.value = EstadoGuardado.Guardando
            try {
                val usuario = autenticacion.currentUser
                val producto = Product(
                    id = idProducto ?: "",
                    name = nombre.value,
                    description = descripcion.value,
                    price = precio.value.toDoubleOrNull() ?: 0.0,
                    condition = condicion.value,
                    categoryId = idCategoria.value,
                    categoryName = _categorias.value.find { it.id == idCategoria.value }?.name ?: "",
                    location = ubicacion.value,
                    tags = etiquetas.value,
                    images = imagenes.value,
                    hasStock = tieneStock.value,
                    stock = if (tieneStock.value) cantidadStock.value.toIntOrNull() ?: 0 else 0,
                    sellerId = usuario?.uid ?: "",
                    sellerName = usuario?.displayName ?: "Vendedor"
                )

                if (idProducto.isNullOrEmpty()) {
                    fuenteProducto.create(producto)
                } else {
                    fuenteProducto.update(producto)
                }
                _estadoGuardado.value = EstadoGuardado.Exito
            } catch (e: Exception) {
                _estadoGuardado.value = EstadoGuardado.Error(e.message ?: "Error al guardar")
            }
        }
    }
}