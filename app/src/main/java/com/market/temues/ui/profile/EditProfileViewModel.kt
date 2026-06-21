package com.market.temues.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.data.remote.user.UserRemoteDataSource
import com.market.temues.data.remote.storage.StorageDataSource
import com.market.temues.model.User
import com.market.temues.ui.seller.EstadoGuardado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val fuenteUsuario: UserRemoteDataSource,
    private val fuenteAlmacenamiento: StorageDataSource,
    private val autenticacion: FirebaseAuth
) : ViewModel() {

    private val idUsuario = autenticacion.currentUser?.uid ?: ""

    private val _usuarioActual = MutableStateFlow<User?>(null)
    val usuarioActual: StateFlow<User?> = _usuarioActual.asStateFlow()

    private val _estadoGuardado = MutableStateFlow<EstadoGuardado>(EstadoGuardado.Inactivo)
    val estadoGuardado: StateFlow<EstadoGuardado> = _estadoGuardado.asStateFlow()

    // Estado separado para la foto para que no cierre la pantalla al terminar de subirla
    private val _estadoFoto = MutableStateFlow<EstadoGuardado>(EstadoGuardado.Inactivo)
    val estadoFoto: StateFlow<EstadoGuardado> = _estadoFoto.asStateFlow()

    init {
        cargarPerfil()
    }

    private fun cargarPerfil() {
        if (idUsuario.isEmpty()) return
        fuenteUsuario.getById(idUsuario)
            .onEach { _usuarioActual.value = it }
            .launchIn(viewModelScope)
    }

    fun actualizarPerfil(nombre: String, telefono: String, biografia: String) {
        viewModelScope.launch {
            _estadoGuardado.value = EstadoGuardado.Guardando
            try {
                val datos = mapOf(
                    "name" to nombre,
                    "phone" to telefono,
                    "bio" to biografia
                )
                fuenteUsuario.update(idUsuario, datos)
                _estadoGuardado.value = EstadoGuardado.Exito
            } catch (e: Exception) {
                _estadoGuardado.value = EstadoGuardado.Error(e.message ?: "Error al actualizar perfil")
            }
        }
    }

    fun actualizarFoto(uri: Uri) {
        viewModelScope.launch {
            _estadoFoto.value = EstadoGuardado.Guardando
            val uidSeguro = autenticacion.currentUser?.uid ?: return@launch
            
            // Log para debug (puedes verlo en Logcat)
            android.util.Log.d("EditProfile", "Iniciando subida para URI: $uri")

            fuenteAlmacenamiento.uploadAvatar(uidSeguro, uri).collect { resultado ->
                resultado.onSuccess { ruta ->
                    fuenteAlmacenamiento.getImageUrl(ruta).collect { resultadoURL ->
                        resultadoURL.onSuccess { url ->
                            fuenteUsuario.update(uidSeguro, mapOf("photoUrl" to url))
                            _estadoFoto.value = EstadoGuardado.Exito
                        }
                    }
                }
                resultado.onFailure { error ->
                    android.util.Log.e("EditProfile", "Error en subida: ${error.message}", error)
                    _estadoFoto.value = EstadoGuardado.Error("Error de permisos: Asegúrate de que la foto esté descargada en tu dispositivo.")
                }
            }
        }
    }
    
    fun reiniciarEstadoGuardado() {
        _estadoGuardado.value = EstadoGuardado.Inactivo
    }

    fun reiniciarEstadoFoto() {
        _estadoFoto.value = EstadoGuardado.Inactivo
    }
}