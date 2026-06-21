package com.market.temues.ui.history

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
class DetalleOrdenViewModel @Inject constructor(
    private val ordenDataSource: OrdenRemoteDataSource,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val ordenId: String = checkNotNull(savedStateHandle["ordenId"])

    private val _uiState = MutableStateFlow<DetalleOrdenUiState>(DetalleOrdenUiState.Cargando)
    val uiState: StateFlow<DetalleOrdenUiState> = _uiState

    sealed class DetalleOrdenUiState {
        data object Cargando : DetalleOrdenUiState()
        data class Exitoso(val orden: Orden) : DetalleOrdenUiState()
        data class Error(val mensaje: String) : DetalleOrdenUiState()
    }

    init {
        cargarDetalle()
    }

    fun cargarDetalle() {
        viewModelScope.launch {
            _uiState.value = DetalleOrdenUiState.Cargando
            try {
                val orden = ordenDataSource.obtenerPorId(ordenId)
                _uiState.value = if (orden != null) {
                    DetalleOrdenUiState.Exitoso(orden)
                } else {
                    DetalleOrdenUiState.Error("No se encontró la orden")
                }
            } catch (e: Exception) {
                _uiState.value = DetalleOrdenUiState.Error(e.message ?: "Error al cargar el detalle")
            }
        }
    }
}
