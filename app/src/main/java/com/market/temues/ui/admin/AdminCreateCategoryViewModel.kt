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

sealed class CreateCategoryUiState {
    data object Idle : CreateCategoryUiState()
    data object Loading : CreateCategoryUiState()
    data class Success(val isUpdate: Boolean) : CreateCategoryUiState()
    data class Error(val message: String) : CreateCategoryUiState()
}

@HiltViewModel
class AdminCreateCategoryViewModel @Inject constructor(
    private val categoryRemoteDataSource: CategoryRemoteDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateCategoryUiState>(CreateCategoryUiState.Idle)
    val uiState: StateFlow<CreateCategoryUiState> = _uiState.asStateFlow()

    private val _categoryName = MutableStateFlow("")
    val categoryName: StateFlow<String> = _categoryName.asStateFlow()

    fun loadCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                val category = categoryRemoteDataSource.getById(categoryId).first()
                _categoryName.value = category?.name ?: ""
            } catch (_: Exception) { }
        }
    }

    fun saveCategory(name: String, categoryId: String?) {
        if (name.isBlank()) {
            _uiState.value = CreateCategoryUiState.Error("El nombre es obligatorio")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateCategoryUiState.Loading
            try {
                if (categoryId != null) {
                    categoryRemoteDataSource.update(
                        Category(id = categoryId, name = name.trim())
                    )
                    _uiState.value = CreateCategoryUiState.Success(true)
                } else {
                    val maxOrder = categoryRemoteDataSource.getMaxOrder()
                    val category = Category(
                        name = name.trim(),
                        order = maxOrder + 1
                    )
                    categoryRemoteDataSource.create(category)
                    _uiState.value = CreateCategoryUiState.Success(false)
                }
            } catch (e: Exception) {
                _uiState.value = CreateCategoryUiState.Error(
                    e.message ?: "Error al guardar categoría"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateCategoryUiState.Idle
    }
}
