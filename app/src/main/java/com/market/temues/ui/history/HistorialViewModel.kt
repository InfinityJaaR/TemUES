package com.market.temues.ui.history

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
class HistorialViewModel @Inject constructor(
    private val ordenDataSource: OrdenRemoteDataSource,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistorialUiState>(HistorialUiState.Cargando)
    val uiState: StateFlow<HistorialUiState> = _uiState

    sealed class HistorialUiState {
        data object Cargando : HistorialUiState()
        data class Exitoso(val ordenes: List<Orden>) : HistorialUiState()
        data class Error(val mensaje: String) : HistorialUiState()
        data object Vacio : HistorialUiState()
    }

    init { cargarHistorial() }

    fun cargarHistorial() {
        val usuarioId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = HistorialUiState.Cargando
            try {
                ordenDataSource.obtenerOrdenesUsuario(usuarioId).collect { ordenes ->
                    _uiState.value = if (ordenes.isEmpty()) {
                        HistorialUiState.Vacio
                    } else {
                        HistorialUiState.Exitoso(ordenes)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = HistorialUiState.Error(e.message ?: "Error al cargar historial")
            }
        }
    }
}
