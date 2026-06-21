package com.market.temues.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.market.temues.data.remote.category.CategoryRemoteDataSource
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.ml.TrendAnalyzer
import com.market.temues.ml.TrendAnalyzer.TrendResult
import com.market.temues.model.Category
import com.market.temues.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryStat(
    val categoryName: String,
    val count: Int,
    val percentage: Float
)

sealed class AdminDashboardUiState {
    data object Loading : AdminDashboardUiState()
    data class Success(
        val totalProducts: Int,
        val productsByCategory: List<CategoryStat>,
        val recentProducts: List<Product>,
        val categoryTrends: List<TrendResult>,
        val categories: List<Category>
    ) : AdminDashboardUiState()
    data class Error(val message: String) : AdminDashboardUiState()
}

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val productRemoteDataSource: ProductRemoteDataSource,
    private val categoryRemoteDataSource: CategoryRemoteDataSource,
    private val trendAnalyzer: TrendAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminDashboardUiState>(AdminDashboardUiState.Loading)
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = AdminDashboardUiState.Loading
            try {
                val allProducts = productRemoteDataSource.getAll().first()
                val categories = categoryRemoteDataSource.getAll().first()

                val activeProducts = allProducts.filter { it.status == "activo" }
                val totalProducts = activeProducts.size

                val byCategory = activeProducts
                    .groupBy { it.categoryName }
                    .map { (name, prods) ->
                        CategoryStat(
                            categoryName = name,
                            count = prods.size,
                            percentage = if (totalProducts > 0) prods.size.toFloat() / totalProducts * 100f else 0f
                        )
                    }
                    .sortedByDescending { it.count }

                val recentProducts = allProducts
                    .sortedByDescending { it.createdAt }
                    .take(5)

                val categoryCounts = activeProducts
                    .groupingBy { it.categoryName }
                    .eachCount()

                val trends = trendAnalyzer.analyze(categoryCounts)

                _uiState.value = AdminDashboardUiState.Success(
                    totalProducts = totalProducts,
                    productsByCategory = byCategory,
                    recentProducts = recentProducts,
                    categoryTrends = trends,
                    categories = categories
                )
            } catch (e: Exception) {
                _uiState.value = AdminDashboardUiState.Error(e.message ?: "Error al cargar dashboard")
            }
        }
    }
}
