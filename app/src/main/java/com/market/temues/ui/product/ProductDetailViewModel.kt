package com.market.temues.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.ui.common.ProductDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRemoteDataSource: ProductRemoteDataSource
) : ViewModel() {
    private val productId: String = savedStateHandle["productId"] ?: ""

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        cargarProducto()
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
