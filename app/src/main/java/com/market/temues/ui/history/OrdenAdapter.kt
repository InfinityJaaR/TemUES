package com.market.temues.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.market.temues.databinding.ItemOrdenBinding
import com.market.temues.model.Orden
import com.market.temues.utils.DateUtils

class OrdenAdapter(
    private val alHacerClic: (Orden) -> Unit
) : ListAdapter<Orden, OrdenAdapter.OrdenViewHolder>(DiffCallback) {

    inner class OrdenViewHolder(private val binding: ItemOrdenBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(orden: Orden) {
            binding.root.setOnClickListener { alHacerClic(orden) }
            binding.txtCodigoOrden.text = orden.codigo
            binding.txtTotalOrden.text = "$%.2f".format(orden.total)
            binding.txtFechaOrden.text = DateUtils.formatTimestamp(orden.creadoEn)

            val cantidadArticulos = orden.articulos.sumOf { it.cantidad }
            binding.txtCantidadArticulos.text = "$cantidadArticulos ${if (cantidadArticulos == 1) "artículo" else "artículos"}"

            val lugar = orden.lugarEntrega.ifBlank { "Coordinar por chat" }
            binding.txtLugarOrden.text = "Punto de entrega: $lugar"

            val (textoEstado, colorEstado) = when (orden.estado) {
                "confirmado"  -> "Confirmado"  to Color.parseColor("#1565C0")
                "entregado"   -> "Entregado"   to Color.parseColor("#2E7D32")
                "cancelado"   -> "Cancelado"   to Color.parseColor("#C62828")
                else          -> "Pendiente"   to Color.parseColor("#E65100")
            }
            binding.txtEstadoOrden.text = textoEstado
            binding.txtEstadoOrden.setTextColor(colorEstado)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdenViewHolder {
        val binding = ItemOrdenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrdenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrdenViewHolder, position: Int) {
        holder.vincular(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Orden>() {
        override fun areItemsTheSame(a: Orden, b: Orden) = a.id == b.id
        override fun areContentsTheSame(a: Orden, b: Orden) = a == b
    }
}
