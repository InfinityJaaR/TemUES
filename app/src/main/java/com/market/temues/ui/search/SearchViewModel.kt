package com.market.temues.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.data.remote.category.CategoryRemoteDataSource
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.model.Category
import com.market.temues.model.Product
import com.market.temues.ui.common.ProductListUiState
import com.market.temues.ui.common.matchesSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class SearchEntry(
    val query: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val productRemoteDataSource: ProductRemoteDataSource,
    categoryRemoteDataSource: CategoryRemoteDataSource,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    val query = MutableStateFlow("")
    val selectedCategoryId = MutableStateFlow("")

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRemoteDataSource.getAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val results: StateFlow<List<Product>> = combine(
        query.debounce(300),
        selectedCategoryId
    ) { texto, categoriaId ->
        texto.trim().lowercase() to categoriaId
    }.flatMapLatest { (texto, categoriaId) ->
        guardarHistorialBusqueda(texto)
        val productosFlow = if (categoriaId.isNotBlank()) {
            productRemoteDataSource.getByCategory(categoriaId)
        } else {
            productRemoteDataSource.getAll()
        }

        productosFlow.map { productos ->
            if (texto.isBlank()) {
                productos
            } else {
                productos.filter { producto -> producto.matchesSearch(texto) }
            }
        }
    }.catch { error ->
        _uiState.value = ProductListUiState.Error(error.message ?: "No se pudieron cargar los productos.")
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var ultimaBusquedaGuardada: String = ""

    init {
        observarResultados()
    }

    fun cargarProductos() {
        _uiState.value = ProductListUiState.Loading
        query.value = query.value
    }

    fun buscar(texto: String) {
        query.value = texto
    }

    fun seleccionarCategoria(categoriaId: String) {
        selectedCategoryId.value = categoriaId
    }

    private fun observarResultados() {
        viewModelScope.launch {
            results.collect { productos ->
                _uiState.value = if (productos.isEmpty()) {
                    ProductListUiState.Empty("No hay productos que coincidan con tu búsqueda o categoría.")
                } else {
                    ProductListUiState.Success(productos)
                }
            }
        }
    }

    private fun guardarHistorialBusqueda(texto: String) {
        if (texto.isBlank() || texto == ultimaBusquedaGuardada) return
        val uid = firebaseAuth.currentUser?.uid ?: return
        ultimaBusquedaGuardada = texto

        viewModelScope.launch {
            runCatching {
                firestore.collection("users")
                    .document(uid)
                    .collection("searchHistory")
                    .add(SearchEntry(query = texto))
                    .await()
            }.onFailure {
                ultimaBusquedaGuardada = ""
            }
        }
    }
}
