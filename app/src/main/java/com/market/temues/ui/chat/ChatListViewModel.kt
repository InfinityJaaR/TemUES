package com.market.temues.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.data.remote.chat.ChatRemoteDataSource
import com.market.temues.data.remote.user.UserRemoteDataSource
import com.market.temues.model.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EstadoListaChat {
    data object Cargando : EstadoListaChat()
    data class Exito(val chats: List<Chat>) : EstadoListaChat()
    data class Vacio(val mensaje: String) : EstadoListaChat()
    data class Error(val mensaje: String) : EstadoListaChat()
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRemoteDataSource: ChatRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _estadoUi = MutableStateFlow<EstadoListaChat>(EstadoListaChat.Cargando)
    val estadoUi: StateFlow<EstadoListaChat> = _estadoUi.asStateFlow()

    private val _nombresUsuarios = MutableStateFlow<Map<String, String>>(emptyMap())
    val nombresUsuarios: StateFlow<Map<String, String>> = _nombresUsuarios.asStateFlow()

    val uidActual: String get() = auth.currentUser?.uid ?: ""

    private var trabajoCarga: Job? = null

    init {
        cargarChats()
    }

    fun cargarChats() {
        if (uidActual.isBlank()) return
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            _estadoUi.value = EstadoListaChat.Cargando
            chatRemoteDataSource.getUserChats(uidActual)
                .catch { error ->
                    _estadoUi.value = EstadoListaChat.Error(error.message ?: "Error al cargar conversaciones.")
                }
                .collect { chats ->
                    if (chats.isEmpty()) {
                        _estadoUi.value = EstadoListaChat.Vacio("No tienes conversaciones.")
                    } else {
                        _estadoUi.value = EstadoListaChat.Exito(chats)
                        cargarNombresParticipantes(chats)
                    }
                }
        }
    }

    private fun cargarNombresParticipantes(chats: List<Chat>) {
        val otrosIds = chats
            .mapNotNull { chat -> chat.participants.firstOrNull { it != uidActual } }
            .filter { it.isNotBlank() }
            .distinct()

        otrosIds.forEach { otroId ->
            viewModelScope.launch {
                userRemoteDataSource.getById(otroId)
                    .catch { }
                    .collect { usuario ->
                        val nombre = usuario?.name?.ifBlank { usuario.email } ?: otroId
                        _nombresUsuarios.value = _nombresUsuarios.value + (otroId to nombre)
                    }
            }
        }
    }

    fun obtenerOtroParticipanteId(chat: Chat): String =
        chat.participants.firstOrNull { it != uidActual } ?: ""
}
