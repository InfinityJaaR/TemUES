package com.market.temues.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.market.temues.R
import com.market.temues.databinding.ItemMensajeAudioEnviadoBinding
import com.market.temues.databinding.ItemMensajeAudioRecibidoBinding
import com.market.temues.databinding.ItemMensajeEnviadoBinding
import com.market.temues.databinding.ItemMensajeRecibidoBinding
import com.market.temues.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MensajesAdapter(
    private val uidActual: String,
    private val alReproducir: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MensajeDiffCallback()) {

    private var idMensajeReproduciendo: String = ""

    companion object {
        private const val TIPO_TEXTO_ENVIADO = 1
        private const val TIPO_TEXTO_RECIBIDO = 2
        private const val TIPO_AUDIO_ENVIADO = 3
        private const val TIPO_AUDIO_RECIBIDO = 4
    }

    fun actualizarReproduccion(nuevoId: String) {
        val idAnterior = idMensajeReproduciendo
        idMensajeReproduciendo = nuevoId
        if (idAnterior.isNotBlank()) {
            val posAnterior = currentList.indexOfFirst { it.id == idAnterior }
            if (posAnterior >= 0) notifyItemChanged(posAnterior)
        }
        if (nuevoId.isNotBlank()) {
            val posNueva = currentList.indexOfFirst { it.id == nuevoId }
            if (posNueva >= 0) notifyItemChanged(posNueva)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val mensaje = getItem(position)
        val esEnviado = mensaje.senderId == uidActual
        return when {
            mensaje.type == "audio" && esEnviado -> TIPO_AUDIO_ENVIADO
            mensaje.type == "audio" -> TIPO_AUDIO_RECIBIDO
            esEnviado -> TIPO_TEXTO_ENVIADO
            else -> TIPO_TEXTO_RECIBIDO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TIPO_TEXTO_ENVIADO -> MensajeTextoEnviadoVH(
                ItemMensajeEnviadoBinding.inflate(inflater, parent, false)
            )
            TIPO_TEXTO_RECIBIDO -> MensajeTextoRecibidoVH(
                ItemMensajeRecibidoBinding.inflate(inflater, parent, false)
            )
            TIPO_AUDIO_ENVIADO -> MensajeAudioEnviadoVH(
                ItemMensajeAudioEnviadoBinding.inflate(inflater, parent, false)
            )
            else -> MensajeAudioRecibidoVH(
                ItemMensajeAudioRecibidoBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = getItem(position)
        when (holder) {
            is MensajeTextoEnviadoVH -> holder.vincular(mensaje)
            is MensajeTextoRecibidoVH -> holder.vincular(mensaje)
            is MensajeAudioEnviadoVH -> holder.vincular(mensaje, idMensajeReproduciendo == mensaje.id)
            is MensajeAudioRecibidoVH -> holder.vincular(mensaje, idMensajeReproduciendo == mensaje.id)
        }
    }

    inner class MensajeTextoEnviadoVH(private val binding: ItemMensajeEnviadoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun vincular(mensaje: Message) {
            binding.txtTextoMensaje.text = mensaje.text
            binding.txtHoraMensaje.text = formatearHora(mensaje.timestamp)
        }
    }

    inner class MensajeTextoRecibidoVH(private val binding: ItemMensajeRecibidoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun vincular(mensaje: Message) {
            binding.txtTextoMensaje.text = mensaje.text
            binding.txtHoraMensaje.text = formatearHora(mensaje.timestamp)
        }
    }

    inner class MensajeAudioEnviadoVH(private val binding: ItemMensajeAudioEnviadoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun vincular(mensaje: Message, reproduciendo: Boolean) {
            binding.txtDuracionAudio.text = formatearDuracion(mensaje.duracionSegundos)
            binding.txtHoraMensaje.text = formatearHora(mensaje.timestamp)
            binding.btnReproducirAudio.setIconResource(
                if (reproduciendo) R.drawable.ic_pause else R.drawable.ic_play
            )
            binding.btnReproducirAudio.setOnClickListener { alReproducir(mensaje) }
        }
    }

    inner class MensajeAudioRecibidoVH(private val binding: ItemMensajeAudioRecibidoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun vincular(mensaje: Message, reproduciendo: Boolean) {
            binding.txtDuracionAudio.text = formatearDuracion(mensaje.duracionSegundos)
            binding.txtHoraMensaje.text = formatearHora(mensaje.timestamp)
            binding.btnReproducirAudio.setIconResource(
                if (reproduciendo) R.drawable.ic_pause else R.drawable.ic_play
            )
            binding.btnReproducirAudio.setOnClickListener { alReproducir(mensaje) }
        }
    }

    private fun formatearHora(timestamp: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

    private fun formatearDuracion(segundos: Int): String {
        val min = segundos / 60
        val seg = segundos % 60
        return "%d:%02d".format(min, seg)
    }
}

class MensajeDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
}
