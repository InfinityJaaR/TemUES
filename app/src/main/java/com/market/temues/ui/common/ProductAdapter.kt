package com.market.temues.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.market.temues.R
import com.market.temues.databinding.ItemProductCardBinding
import com.market.temues.model.Product

class ProductAdapter(
    private val cargarImagen: (String?, ItemProductCardBinding) -> Unit,
    private val alSeleccionarProducto: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(producto: Product) {
            binding.txtProductName.text = producto.name
            binding.txtProductMeta.text = binding.root.context.getString(
                R.string.product_card_meta,
                producto.location,
                producto.condition
            )
            binding.txtProductPrice.text = binding.root.context.getString(R.string.product_price_format, producto.price)
            binding.imgProduct.setImageResource(R.drawable.bg_soft_card)
            cargarImagen(producto.images.firstOrNull(), binding)
            binding.root.setOnClickListener { alSeleccionarProducto(producto) }
        }
    }

    private object ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem == newItem
    }
}
