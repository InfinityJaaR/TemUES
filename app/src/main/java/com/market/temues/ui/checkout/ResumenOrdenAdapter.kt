package com.market.temues.ui.checkout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.market.temues.data.local.entity.CarritoEntidad
import com.market.temues.databinding.ItemResumenOrdenBinding

class ResumenOrdenAdapter : ListAdapter<CarritoEntidad, ResumenOrdenAdapter.ResumenViewHolder>(DiffCallback) {

    inner class ResumenViewHolder(private val binding: ItemResumenOrdenBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(articulo: CarritoEntidad) {
            binding.txtCantidadResumen.text = "${articulo.cantidad}x"
            binding.txtNombreResumen.text = articulo.nombreProducto
            binding.txtPrecioResumen.text = "$%.2f".format(articulo.precio * articulo.cantidad)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResumenViewHolder {
        val binding = ItemResumenOrdenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ResumenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResumenViewHolder, position: Int) {
        holder.vincular(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CarritoEntidad>() {
        override fun areItemsTheSame(a: CarritoEntidad, b: CarritoEntidad) =
            a.productoId == b.productoId

        override fun areContentsTheSame(a: CarritoEntidad, b: CarritoEntidad) = a == b
    }
}
