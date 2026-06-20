package com.market.temues.ui.common

import com.market.temues.model.Product

sealed class ProductListUiState {
    data object Loading : ProductListUiState()
    data class Success(val products: List<Product>) : ProductListUiState()
    data class Empty(val message: String) : ProductListUiState()
    data class Error(val message: String) : ProductListUiState()
}
