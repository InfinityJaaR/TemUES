package com.market.temues.ui.chat

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.data.remote.chat.ChatRemoteDataSource
import com.market.temues.data.remote.chat.MessageRemoteDataSource
import com.market.temues.data.remote.user.UserRemoteDataSource
import com.market.temues.R
import com.market.temues.model.Chat
import com.market.temues.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class EstadoDetalleChat {
    data object Cargando : EstadoDetalleChat()
    data class Exito(val chat: Chat?, val mensajes: List<Message>) : EstadoDetalleChat()
    data class Error(val mensaje: String) : EstadoDetalleChat()
}

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val contexto: Context,
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

    private val _estaGrabando = MutableStateFlow(false)
    val estaGrabando: StateFlow<Boolean> = _estaGrabando.asStateFlow()

    private val _segundosGrabacion = MutableStateFlow(0)
    val segundosGrabacion: StateFlow<Int> = _segundosGrabacion.asStateFlow()

    private val _idMensajeReproduciendo = MutableStateFlow("")
    val idMensajeReproduciendo: StateFlow<String> = _idMensajeReproduciendo.asStateFlow()

    private val _eventoUi = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val eventoUi: SharedFlow<String> = _eventoUi.asSharedFlow()

    val uidActual: String get() = auth.currentUser?.uid ?: ""

    private var chatActual: Chat? = null
    private var mensajesActuales: List<Message> = emptyList()

    private var mediaRecorder: MediaRecorder? = null
    private var archivoGrabacion: File? = null
    private var trabajoDuracion: Job? = null

    private var mediaPlayer: MediaPlayer? = null

    init {
        if (chatId.isNotBlank()) {
            observarChat()
            observarMensajes()
            resetearNoLeidos()
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
                    _telefonoOtroUsuario.value = usuario?.phone ?: ""
                }
        }
    }

    fun enviarMensaje(texto: String) {
        if (texto.isBlank() || chatId.isBlank()) return
        val otroId = chatActual?.participants?.firstOrNull { it != uidActual } ?: return

        viewModelScope.launch {
            try {
                val mensaje = Message(
                    chatId = chatId,
                    senderId = uidActual,
                    type = "text",
                    text = texto.trim(),
                    timestamp = System.currentTimeMillis()
                )
                messageRemoteDataSource.send(chatId, mensaje)
                chatRemoteDataSource.updateLastMessage(chatId, texto.trim(), uidActual)
                chatRemoteDataSource.incrementarNoLeidos(chatId, otroId)
                dispararNotificacion(otroId, texto.trim())
            } catch (_: Exception) { }
        }
    }

    fun iniciarGrabacion() {
        if (_estaGrabando.value) return
        val archivo = File(contexto.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        try {
            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(contexto)
            } else {
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(archivo.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            archivoGrabacion = archivo
            _segundosGrabacion.value = 0
            _estaGrabando.value = true

            trabajoDuracion = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _segundosGrabacion.value++
                }
            }
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            archivo.delete()
            _eventoUi.tryEmit(contexto.getString(R.string.chat_error_grabacion))
        }
    }

    fun detenerYEnviarAudio() {
        if (!_estaGrabando.value) return
        trabajoDuracion?.cancel()
        trabajoDuracion = null
        val duracion = _segundosGrabacion.value
        _estaGrabando.value = false

        try {
            mediaRecorder?.stop()
        } catch (_: Exception) { }
        mediaRecorder?.release()
        mediaRecorder = null

        if (duracion < 1) {
            archivoGrabacion?.delete()
            archivoGrabacion = null
            return
        }

        val archivoAEnviar = archivoGrabacion ?: return
        archivoGrabacion = null
        val chatIdCopia = chatId
        val otroId = chatActual?.participants?.firstOrNull { it != uidActual } ?: return

        viewModelScope.launch {
            try {
                val urlAudio = messageRemoteDataSource.subirAudio(chatIdCopia, archivoAEnviar)
                val mensaje = Message(
                    chatId = chatIdCopia,
                    senderId = uidActual,
                    type = "audio",
                    audioUrl = urlAudio,
                    duracionSegundos = duracion,
                    timestamp = System.currentTimeMillis()
                )
                messageRemoteDataSource.send(chatIdCopia, mensaje)
                chatRemoteDataSource.updateLastMessage(chatIdCopia, "🎵 Audio", uidActual)
                chatRemoteDataSource.incrementarNoLeidos(chatIdCopia, otroId)
                dispararNotificacion(otroId, "🎵 Audio")
            } catch (e: Exception) {
                _eventoUi.tryEmit(contexto.getString(R.string.chat_error_envio_audio))
            } finally {
                archivoAEnviar.delete()
            }
        }
    }

    fun cancelarGrabacion() {
        if (!_estaGrabando.value) return
        trabajoDuracion?.cancel()
        trabajoDuracion = null
        _estaGrabando.value = false
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) { }
        mediaRecorder?.release()
        mediaRecorder = null
        archivoGrabacion?.delete()
        archivoGrabacion = null
    }

    fun toggleReproduccion(mensaje: Message) {
        if (mensaje.audioUrl.isBlank()) return

        if (_idMensajeReproduciendo.value == mensaje.id) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _idMensajeReproduciendo.value = ""
            } else {
                mediaPlayer?.start()
                _idMensajeReproduciendo.value = mensaje.id
            }
            return
        }

        liberarReproductor()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(mensaje.audioUrl)
            setOnPreparedListener {
                start()
                _idMensajeReproduciendo.value = mensaje.id
            }
            setOnCompletionListener {
                _idMensajeReproduciendo.value = ""
            }
            setOnErrorListener { _, _, _ ->
                _idMensajeReproduciendo.value = ""
                true
            }
            prepareAsync()
        }
    }

    private fun liberarReproductor() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _idMensajeReproduciendo.value = ""
    }

    private fun dispararNotificacion(otroId: String, texto: String) {
        val nombreRemitente = auth.currentUser?.displayName
            ?: auth.currentUser?.email
            ?: ""
        val datosNotificacion = mapOf(
            "type" to "chat_message",
            "chatId" to chatId,
            "senderId" to uidActual,
            "senderName" to nombreRemitente,
            "text" to texto,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("users").document(otroId)
            .collection("notifications")
            .add(datosNotificacion)
    }

    private fun resetearNoLeidos() {
        val uid = uidActual
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                chatRemoteDataSource.resetearNoLeidos(chatId, uid)
            } catch (_: Exception) { }
        }
    }

    private fun marcarMensajesRecibidosComoLeidos(mensajes: List<Message>) {
        val noLeidos = mensajes.filter { it.senderId != uidActual && !it.isRead }
        if (noLeidos.isEmpty()) return
        resetearNoLeidos()
        noLeidos.forEach { mensaje ->
            viewModelScope.launch {
                try {
                    messageRemoteDataSource.markAsRead(chatId, mensaje.id)
                } catch (_: Exception) { }
            }
        }
    }

    fun marcarComoLeido(mensajeId: String) {
        viewModelScope.launch {
            try {
                messageRemoteDataSource.markAsRead(chatId, mensajeId)
            } catch (_: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelarGrabacion()
        liberarReproductor()
    }
}
