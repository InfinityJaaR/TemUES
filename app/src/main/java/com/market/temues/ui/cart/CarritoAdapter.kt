package com.market.temues.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.market.temues.data.local.entity.CarritoEntidad
import com.market.temues.databinding.ItemCarritoBinding

class CarritoAdapter(
    private val onAumentar: (String) -> Unit,
    private val onDisminuir: (String) -> Unit,
    private val onEliminar: (String) -> Unit
) : ListAdapter<CarritoEntidad, CarritoAdapter.CarritoViewHolder>(DiffCallback) {

    inner class CarritoViewHolder(private val binding: ItemCarritoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(articulo: CarritoEntidad) {
            binding.txtNombreProducto.text = articulo.nombreProducto
            binding.txtPrecioUnitario.text = "$%.2f c/u".format(articulo.precio)
            binding.txtCantidad.text = articulo.cantidad.toString()
            binding.txtSubtotal.text = "$%.2f".format(articulo.precio * articulo.cantidad)

            Glide.with(binding.root)
                .load(articulo.urlImagen)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(binding.imgProducto)

            binding.btnAumentar.setOnClickListener { onAumentar(articulo.productoId) }
            binding.btnDisminuir.setOnClickListener { onDisminuir(articulo.productoId) }
            binding.btnEliminar.setOnClickListener { onEliminar(articulo.productoId) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val binding = ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarritoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        holder.vincular(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CarritoEntidad>() {
        override fun areItemsTheSame(a: CarritoEntidad, b: CarritoEntidad) =
            a.productoId == b.productoId

        override fun areContentsTheSame(a: CarritoEntidad, b: CarritoEntidad) = a == b
    }
}
