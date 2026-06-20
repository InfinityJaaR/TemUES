package com.market.temues.ui.common

import com.market.temues.model.Product

sealed class ProductDetailUiState {
    data object Loading : ProductDetailUiState()
    data class Success(val product: Product) : ProductDetailUiState()
    data class Empty(val message: String) : ProductDetailUiState()
    data class Error(val message: String) : ProductDetailUiState()
}
