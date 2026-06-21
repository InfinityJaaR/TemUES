package com.market.temues.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.data.remote.chat.ChatRemoteDataSource
import com.market.temues.data.remote.chat.MessageRemoteDataSource
import com.market.temues.data.remote.user.UserRemoteDataSource
import com.market.temues.model.Chat
import com.market.temues.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EstadoDetalleChat {
    data object Cargando : EstadoDetalleChat()
    data class Exito(val chat: Chat?, val mensajes: List<Message>) : EstadoDetalleChat()
    data class Error(val mensaje: String) : EstadoDetalleChat()
}

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val chatRemoteDataSource: ChatRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val chatId: String = savedStateHandle["chatId"] ?: ""

    private val _estadoUi = MutableStateFlow<EstadoDetalleChat>(EstadoDetalleChat.Cargando)
    val estadoUi: StateFlow<EstadoDetalleChat> = _estadoUi.asStateFlow()

    private val _nombreOtroUsuario = MutableStateFlow("")
    val nombreOtroUsuario: StateFlow<String> = _nombreOtroUsuario.asStateFlow()

    private val _telefonoOtroUsuario = MutableStateFlow("")
    val telefonoOtroUsuario: StateFlow<String> = _telefonoOtroUsuario.asStateFlow()

    val uidActual: String get() = auth.currentUser?.uid ?: ""

    private var chatActual: Chat? = null
    private var mensajesActuales: List<Message> = emptyList()

    init {
        if (chatId.isNotBlank()) {
            observarChat()
            observarMensajes()
        }
    }

    private fun observarChat() {
        viewModelScope.launch {
            chatRemoteDataSource.getById(chatId)
                .catch { }
                .collect { chat ->
                    chatActual = chat
                    actualizarEstado()
                    chat?.let { cargarDatosOtroUsuario(it) }
                }
        }
    }

    private fun observarMensajes() {
        viewModelScope.launch {
            messageRemoteDataSource.getMessages(chatId)
                .catch { error ->
                    _estadoUi.value = EstadoDetalleChat.Error(error.message ?: "Error al cargar mensajes.")
                }
                .collect { mensajes ->
                    mensajesActuales = mensajes
                    actualizarEstado()
                    marcarMensajesRecibidosComoLeidos(mensajes)
                }
        }
    }

    private fun actualizarEstado() {
        _estadoUi.value = EstadoDetalleChat.Exito(chatActual, mensajesActuales)
    }

    private fun cargarDatosOtroUsuario(chat: Chat) {
        val otroId = chat.participants.firstOrNull { it != uidActual } ?: return
        viewModelScope.launch {
            userRemoteDataSource.getById(otroId)
                .catch { }
                .collect { usuario ->
                    _nombreOtroUsuario.value = usuario?.name?.ifBlank { usuario.email } ?: ""
                    _telefonoOtroUsuario.value = ""
                }
        }
    }

    fun enviarMensaje(texto: String) {
        if (texto.isBlank() || chatId.isBlank()) return
        val otroId = chatActual?.participants?.firstOrNull { it != uidActual } ?: return

        viewModelScope.launch {
            val mensaje = Message(
                chatId = chatId,
                senderId = uidActual,
                text = texto.trim(),
                timestamp = System.currentTimeMillis()
            )
            messageRemoteDataSource.send(chatId, mensaje)
            chatRemoteDataSource.updateLastMessage(chatId, texto.trim(), uidActual)
            dispararNotificacion(otroId, texto.trim())
        }
    }

    private fun dispararNotificacion(otroId: String, texto: String) {
        val datosNotificacion = mapOf(
            "type" to "chat_message",
            "chatId" to chatId,
            "senderId" to uidActual,
            "text" to texto,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("users").document(otroId)
            .collection("notifications")
            .add(datosNotificacion)
    }

    private fun marcarMensajesRecibidosComoLeidos(mensajes: List<Message>) {
        mensajes
            .filter { it.senderId != uidActual && !it.isRead }
            .forEach { mensaje ->
                viewModelScope.launch {
                    messageRemoteDataSource.markAsRead(chatId, mensaje.id)
                }
            }
    }

    fun marcarComoLeido(mensajeId: String) {
        viewModelScope.launch {
            messageRemoteDataSource.markAsRead(chatId, mensajeId)
        }
    }
}
