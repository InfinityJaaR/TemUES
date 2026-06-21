package com.market.temues.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.data.remote.FirestoreSeeder
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.ml.RecommendationEngine
import com.market.temues.model.Product
import com.market.temues.ui.common.ProductListUiState
import com.market.temues.ui.common.matchesSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRemoteDataSource: ProductRemoteDataSource,
    private val recommendationEngine: RecommendationEngine,
    private val firebaseAuth: FirebaseAuth,
    private val firestoreSeeder: FirestoreSeeder
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val _rankedProducts = MutableStateFlow<List<Product>>(emptyList())
    val rankedProducts: StateFlow<List<Product>> = _rankedProducts.asStateFlow()

    private var productosFirestore: List<Product> = emptyList()
    private var textoBusqueda: String = ""
    private var categoriaSeleccionada: String = ""
    private var cargaIniciada = false

    fun cargarProductos(forzarRecarga: Boolean = false) {
        if (cargaIniciada && !forzarRecarga) return
        cargaIniciada = true
        viewModelScope.launch {
            _uiState.value = ProductListUiState.Loading
            runCatching { firestoreSeeder.seed() }
            productRemoteDataSource.getAll()
                .catch { error ->
                    _uiState.value = ProductListUiState.Error(error.message ?: "No se pudieron cargar los productos.")
                }
                .collect { products ->
                    productosFirestore = runCatching {
                        recommendationEngine.rankProducts(products, firebaseAuth.currentUser?.uid)
                    }.getOrElse { products }
                    _rankedProducts.value = productosFirestore
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

    fun actualizarRanking() {
        if (productosFirestore.isEmpty()) return

        viewModelScope.launch {
            productosFirestore = runCatching {
                recommendationEngine.rankProducts(productosFirestore, firebaseAuth.currentUser?.uid)
            }.getOrElse { productosFirestore }
            _rankedProducts.value = productosFirestore
            aplicarFiltros()
        }
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
