package com.market.temues.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.market.temues.databinding.ItemArticuloOrdenBinding
import com.market.temues.model.ArticuloOrden

class ArticuloOrdenAdapter : ListAdapter<ArticuloOrden, ArticuloOrdenAdapter.ArticuloViewHolder>(DiffCallback) {

    inner class ArticuloViewHolder(private val binding: ItemArticuloOrdenBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(articulo: ArticuloOrden) {
            binding.txtNombreArticulo.text = articulo.nombreProducto
            binding.txtPrecioUnitario.text = "$%.2f c/u".format(articulo.precio)
            binding.txtCantidadArticulo.text = "x${articulo.cantidad}"
            binding.txtSubtotalArticulo.text = "$%.2f".format(articulo.precio * articulo.cantidad)

            if (articulo.urlImagen.isNotBlank()) {
                Glide.with(binding.imgArticulo)
                    .load(articulo.urlImagen)
                    .centerCrop()
                    .into(binding.imgArticulo)
            } else {
                binding.imgArticulo.setImageDrawable(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticuloViewHolder {
        val binding = ItemArticuloOrdenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ArticuloViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticuloViewHolder, position: Int) {
        holder.vincular(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ArticuloOrden>() {
        override fun areItemsTheSame(a: ArticuloOrden, b: ArticuloOrden) =
            a.productoId == b.productoId
        override fun areContentsTheSame(a: ArticuloOrden, b: ArticuloOrden) = a == b
    }
}
