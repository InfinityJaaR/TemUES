package com.market.temues.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.market.temues.databinding.ItemMensajeEnviadoBinding
import com.market.temues.databinding.ItemMensajeRecibidoBinding
import com.market.temues.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MensajesAdapter(
    private val uidActual: String
) : ListAdapter<Message, RecyclerView.ViewHolder>(MensajeDiffCallback()) {

    companion object {
        private const val TIPO_ENVIADO = 1
        private const val TIPO_RECIBIDO = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == uidActual) TIPO_ENVIADO else TIPO_RECIBIDO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_ENVIADO) {
            val binding = ItemMensajeEnviadoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            MensajeEnviadoViewHolder(binding)
        } else {
            val binding = ItemMensajeRecibidoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            MensajeRecibidoViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = getItem(position)
        when (holder) {
            is MensajeEnviadoViewHolder -> holder.vincular(mensaje)
            is MensajeRecibidoViewHolder -> holder.vincular(mensaje)
        }
    }

    inner class MensajeEnviadoViewHolder(private val binding: ItemMensajeEnviadoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(mensaje: Message) {
            binding.txtTextoMensaje.text = mensaje.text
            binding.txtHoraMensaje.text = formatearHora(mensaje.timestamp)
        }
    }

    inner class MensajeRecibidoViewHolder(private val binding: ItemMensajeRecibidoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(mensaje: Message) {
            binding.txtTextoMensaje.text = mensaje.text
            binding.txtHoraMensaje.text = formatearHora(mensaje.timestamp)
        }
    }

    private fun formatearHora(timestamp: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

class MensajeDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
}
