package com.market.temues.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.market.temues.data.remote.category.CategoryRemoteDataSource
import com.market.temues.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry
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

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                categoryRemoteDataSource.delete(categoryId)
            } catch (e: Exception) {
                _uiState.value = CategoryListUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRemoteDataSource.getAll()
                .retry(3)
                .catch { e ->
                    _uiState.value = CategoryListUiState.Error(e.message ?: "Error desconocido")
                }
                .collect { categories ->
                    _uiState.value = CategoryListUiState.Success(categories)
                }
        }
    }
}
