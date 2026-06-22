package com.market.temues.ui.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.data.remote.orden.OrdenRemoteDataSource
import com.market.temues.model.Orden
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistorialVentasViewModel @Inject constructor(
    private val ordenDataSource: OrdenRemoteDataSource,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistorialVentasUiState>(HistorialVentasUiState.Cargando)
    val uiState: StateFlow<HistorialVentasUiState> = _uiState

    sealed class HistorialVentasUiState {
        data object Cargando : HistorialVentasUiState()
        data class Exitoso(val ordenes: List<Orden>) : HistorialVentasUiState()
        data class Error(val mensaje: String) : HistorialVentasUiState()
        data object Vacio : HistorialVentasUiState()
    }

    init { cargarVentas() }

    fun cargarVentas() {
        val vendedorId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = HistorialVentasUiState.Cargando
            try {
                ordenDataSource.obtenerOrdenesVendedor(vendedorId).collect { ordenes ->
                    _uiState.value = if (ordenes.isEmpty()) {
                        HistorialVentasUiState.Vacio
                    } else {
                        HistorialVentasUiState.Exitoso(ordenes)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = HistorialVentasUiState.Error(e.message ?: "Error al cargar ventas")
            }
        }
    }
}
