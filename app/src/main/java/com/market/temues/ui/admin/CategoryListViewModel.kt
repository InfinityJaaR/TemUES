package com.market.temues.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.market.temues.data.remote.category.CategoryRemoteDataSource
import com.market.temues.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CategoryListUiState {
    data object Loading : CategoryListUiState()
    data class Success(val categories: List<Category>) : CategoryListUiState()
    data class Error(val message: String) : CategoryListUiState()
}

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val categoryRemoteDataSource: CategoryRemoteDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryListUiState>(CategoryListUiState.Loading)
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = CategoryListUiState.Loading
            try {
                val categories = categoryRemoteDataSource.getAll().first()
                _uiState.value = CategoryListUiState.Success(categories)
            } catch (e: Exception) {
                _uiState.value = CategoryListUiState.Error(
                    e.message ?: "Error al cargar categorías"
                )
            }
        }
    }
}
