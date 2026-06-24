package com.market.temues.ui.seller

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.market.temues.data.remote.orden.OrdenRemoteDataSource
import com.market.temues.model.Orden
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellerSaleDetailViewModel @Inject constructor(
    private val ordenDataSource: OrdenRemoteDataSource,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val ordenId: String = checkNotNull(savedStateHandle["ordenId"])

    private val _uiState = MutableStateFlow<SellerSaleDetailUiState>(SellerSaleDetailUiState.Cargando)
    val uiState: StateFlow<SellerSaleDetailUiState> = _uiState

    sealed class SellerSaleDetailUiState {
        data object Cargando : SellerSaleDetailUiState()
        data class Exitoso(val orden: Orden) : SellerSaleDetailUiState()
        data class Error(val mensaje: String) : SellerSaleDetailUiState()
    }

    init {
        cargarDetalle()
    }

    fun cargarDetalle() {
        viewModelScope.launch {
            _uiState.value = SellerSaleDetailUiState.Cargando
            try {
                val orden = ordenDataSource.obtenerPorId(ordenId)
                _uiState.value = if (orden != null) {
                    SellerSaleDetailUiState.Exitoso(orden)
                } else {
                    SellerSaleDetailUiState.Error("No se encontró la venta")
                }
            } catch (e: Exception) {
                _uiState.value = SellerSaleDetailUiState.Error(e.message ?: "Error al cargar el detalle")
            }
        }
    }

    fun marcarEntregada() {
        viewModelScope.launch {
            try {
                ordenDataSource.actualizarEstado(ordenId, "entregado")
            } catch (_: Exception) { }
        }
    }
}
