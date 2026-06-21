package com.market.temues.ui.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.market.temues.R
import com.market.temues.databinding.ItemSellerProductBinding
import com.market.temues.model.Product
import com.market.temues.utils.DateUtils

class SellerProductAdapter(
    private val alEditar: (Product) -> Unit,
    private val alEliminar: (Product) -> Unit,
    private val alCambiarEstado: (Product) -> Unit
) : ListAdapter<Product, SellerProductAdapter.ViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSellerProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.vincular(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSellerProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(producto: Product) {
            binding.tvProductName.text = producto.name
            // Formatear precio con 2 decimales
            binding.tvProductPrice.text = "$%.2f".format(producto.price)
            
            // Mostrar tiempo relativo de publicación
            val tiempoPublicacion = DateUtils.getRelativeTime(producto.createdAt)
            // Podríamos agregarlo a un TextView si existiera, o concatenarlo al nombre por ahora
            // para demostrar el uso de la utilidad
            binding.tvProductName.text = "${producto.name} ($tiempoPublicacion)"
            
            val recursoEstado = if (producto.status == "activo") R.string.product_status_active else R.string.product_status_sold
            binding.chipStatus.setText(recursoEstado)
            
            // Texto del botón: Si está activo, mostrar "Marcar como vendido". Si está vendido, mostrar "Reactivar".
            val recursoBoton = if (producto.status == "activo") R.string.product_action_mark_sold else R.string.product_action_reactivate
            binding.btnToggleStatus.setText(recursoBoton)

            Glide.with(binding.ivProduct)
                .load(producto.images.firstOrNull())
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(binding.ivProduct)

            binding.btnEdit.setOnClickListener { alEditar(producto) }
            binding.btnDelete.setOnClickListener { alEliminar(producto) }
            binding.btnToggleStatus.setOnClickListener { alCambiarEstado(producto) }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean =
            oldItem == newItem
    }
}