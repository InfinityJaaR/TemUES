package com.market.temues.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.data.remote.chat.ChatRemoteDataSource
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.data.repository.FavoritesRepository
import com.market.temues.ui.common.ProductDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRemoteDataSource: ProductRemoteDataSource,
    private val chatRemoteDataSource: ChatRemoteDataSource,
    private val favoritesRepository: FavoritesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val productId: String = savedStateHandle["productId"] ?: ""
    private val userId = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    val esFavorito: StateFlow<Boolean> = if (userId.isNotEmpty() && productId.isNotEmpty()) {
        favoritesRepository.isFavorite(userId, productId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    } else {
        MutableStateFlow(false).asStateFlow()
    }

    init {
        cargarProducto()
    }

    private val _cargandoChat = MutableStateFlow(false)
    val cargandoChat: StateFlow<Boolean> = _cargandoChat.asStateFlow()

    fun esPropioProducto(): Boolean {
        val producto = (uiState.value as? ProductDetailUiState.Success)?.product ?: return false
        return producto.sellerId == userId
    }

    suspend fun crearOAbrirChat(): String {
        val producto = (uiState.value as? ProductDetailUiState.Success)?.product ?: return ""
        if (userId.isBlank() || producto.sellerId.isBlank()) return ""
        if (userId == producto.sellerId) return ""
        _cargandoChat.value = true
        return try {
            chatRemoteDataSource.createOrGet(userId, producto.sellerId, producto.id)
        } catch (_: Exception) {
            ""
        } finally {
            _cargandoChat.value = false
        }
    }

    fun alternarFavorito() {
        val estadoActual = uiState.value
        if (estadoActual is ProductDetailUiState.Success && userId.isNotEmpty()) {
            viewModelScope.launch {
                favoritesRepository.toggleFavorite(userId, estadoActual.product)
            }
        }
    }

    private fun cargarProducto() {
        if (productId.isBlank()) {
            _uiState.value = ProductDetailUiState.Error("No se encontró el producto solicitado.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProductDetailUiState.Loading
            productRemoteDataSource.getById(productId)
                .catch { error ->
                    _uiState.value = ProductDetailUiState.Error(error.message ?: "No se pudo cargar el producto.")
                }
                .collect { product ->
                    _uiState.value = product?.let { ProductDetailUiState.Success(it) }
                        ?: ProductDetailUiState.Empty("El producto ya no está disponible.")
                }
        }
    }
}