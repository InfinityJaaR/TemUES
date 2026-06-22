package com.market.temues.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.market.temues.databinding.ItemListaChatBinding
import com.market.temues.model.Chat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val uidActual: String,
    private val alHacerClickEnChat: (Chat) -> Unit
) : ListAdapter<Chat, ChatListAdapter.ChatViewHolder>(ChatDiffCallback()) {

    private var nombresUsuarios: Map<String, String> = emptyMap()

    fun actualizarNombres(nombres: Map<String, String>) {
        nombresUsuarios = nombres
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemListaChatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = getItem(position)
        val otroId = chat.participants.firstOrNull { it != uidActual } ?: ""
        val nombre = nombresUsuarios[otroId] ?: "Cargando..."
        holder.vincular(chat, nombre)
    }

    inner class ChatViewHolder(private val binding: ItemListaChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(chat: Chat, nombreOtroUsuario: String) {
            binding.txtNombreUsuario.text = nombreOtroUsuario.ifBlank { "Usuario" }
            binding.txtUltimoMensaje.text = chat.lastMessage.ifBlank { "Sin mensajes aún" }
            binding.txtTiempoRelativo.text = calcularTiempoRelativo(chat.lastMessageTimestamp)
            binding.txtInicialAvatar.text = nombreOtroUsuario.firstOrNull()?.uppercase() ?: "?"
            binding.root.setOnClickListener { alHacerClickEnChat(chat) }
        }
    }

    private fun calcularTiempoRelativo(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val ahora = System.currentTimeMillis()
        val diferencia = ahora - timestamp
        return when {
            diferencia < 60_000 -> "ahora"
            diferencia < 3_600_000 -> "${diferencia / 60_000} min"
            diferencia < 86_400_000 -> "${diferencia / 3_600_000}h"
            diferencia < 604_800_000 -> "${diferencia / 86_400_000}d"
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
    override fun areItemsTheSame(oldItem: Chat, newItem: Chat) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Chat, newItem: Chat) = oldItem == newItem
}
