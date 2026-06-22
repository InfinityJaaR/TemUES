package com.market.temues.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.data.remote.chat.ChatRemoteDataSource
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.data.remote.user.UserRemoteDataSource
import com.market.temues.data.repository.FavoritesRepository
import com.market.temues.model.Product
import com.market.temues.model.User
import com.market.temues.ui.common.ProductDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProductDetailEvent {
    data class ToggleFavorite(val productId: String, val isFavorite: Boolean) : ProductDetailEvent()
    data class OpenChat(val sellerId: String, val productId: String) : ProductDetailEvent()
    data class AddToCart(val productId: String) : ProductDetailEvent()
    data class BuyNow(val productId: String) : ProductDetailEvent()
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRemoteDataSource: ProductRemoteDataSource,
    private val chatRemoteDataSource: ChatRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val favoritesRepository: FavoritesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val productId: String = savedStateHandle["productId"] ?: ""
    private val userId = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _seller = MutableStateFlow<User?>(null)
    val seller: StateFlow<User?> = _seller.asStateFlow()

    private var sellerIdCargado: String = ""

    private val _events = Channel<ProductDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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
            chatRemoteDataSource.createOrGet(
            participant1 = userId,
            participant2 = producto.sellerId,
            productId = producto.id,
            productName = producto.name,
            productImage = producto.images.firstOrNull() ?: "",
            productPrice = producto.price
        )
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

    fun onFavoriteClicked() {
        val producto = _product.value ?: return
        val nuevoEstadoFavorito = !esFavorito.value
        viewModelScope.launch {
            if (userId.isNotEmpty()) {
                favoritesRepository.toggleFavorite(userId, producto)
            }
            _events.send(ProductDetailEvent.ToggleFavorite(producto.id, nuevoEstadoFavorito))
        }
    }

    fun onChatClicked() {
        val producto = _product.value ?: return
        viewModelScope.launch {
            _events.send(ProductDetailEvent.OpenChat(producto.sellerId, producto.id))
        }
    }

    fun onAddToCartClicked() {
        val producto = _product.value ?: return
        viewModelScope.launch {
            _events.send(ProductDetailEvent.AddToCart(producto.id))
        }
    }

    fun onBuyNowClicked() {
        val producto = _product.value ?: return
        viewModelScope.launch {
            _events.send(ProductDetailEvent.BuyNow(producto.id))
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
                    _product.value = product
                    product?.sellerId?.takeIf { it.isNotBlank() && it != sellerIdCargado }?.let { sellerId ->
                        cargarVendedor(sellerId)
                    }
                    _uiState.value = product?.let { ProductDetailUiState.Success(it) }
                        ?: ProductDetailUiState.Empty("El producto ya no está disponible.")
                }
        }
    }

    private fun cargarVendedor(sellerId: String) {
        sellerIdCargado = sellerId
        viewModelScope.launch {
            userRemoteDataSource.getById(sellerId)
                .catch { _seller.value = null }
                .collect { user -> _seller.value = user }
        }
    }
}
