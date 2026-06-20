package com.market.temues.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.model.Product
import com.market.temues.ui.common.ProductListUiState
import com.market.temues.ui.common.matchesSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val productRemoteDataSource: ProductRemoteDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private var productosFirestore: List<Product> = emptyList()
    private var textoBusqueda: String = ""
    private var categoriaSeleccionada: String = ""
    private var trabajoCarga: Job? = null

    fun cargarProductos() {
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            _uiState.value = ProductListUiState.Loading
            productRemoteDataSource.getAll()
                .catch { error ->
                    _uiState.value = ProductListUiState.Error(error.message ?: "No se pudieron cargar los productos.")
                }
                .collect { productos ->
                    productosFirestore = productos
                    aplicarFiltros()
                }
        }
    }

    fun buscar(texto: String) {
        textoBusqueda = texto.trim().lowercase()
        aplicarFiltros()
    }

    fun seleccionarCategoria(categoriaId: String) {
        categoriaSeleccionada = categoriaId
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        val productosFiltrados = productosFirestore.filter { producto ->
            val coincideTexto = textoBusqueda.isBlank() || producto.matchesSearch(textoBusqueda)
            val coincideCategoria = categoriaSeleccionada.isBlank() || producto.categoryId == categoriaSeleccionada
            coincideTexto && coincideCategoria
        }

        _uiState.value = if (productosFiltrados.isEmpty()) {
            ProductListUiState.Empty("No hay productos que coincidan con tu búsqueda o categoría.")
        } else {
            ProductListUiState.Success(productosFiltrados)
        }
    }
}
