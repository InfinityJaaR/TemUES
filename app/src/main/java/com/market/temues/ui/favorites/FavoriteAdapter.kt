package com.market.temues.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.market.temues.databinding.ItemFavoriteProductBinding
import com.market.temues.data.local.entity.FavoriteEntity

class FavoriteAdapter(
    private val onClick: (FavoriteEntity) -> Unit,
    private val onRemove: (FavoriteEntity) -> Unit
) : ListAdapter<FavoriteEntity, FavoriteAdapter.ViewHolder>(FavoriteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavoriteProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFavoriteProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(favorite: FavoriteEntity) {
            binding.tvProductName.text = favorite.productName
            // Formatear precio con 2 decimales
            binding.tvProductPrice.text = "$%.2f".format(favorite.productPrice)

            Glide.with(binding.ivProduct)
                .load(favorite.productImage)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(binding.ivProduct)

            binding.root.setOnClickListener { onClick(favorite) }
            binding.btnRemoveFavorite.setOnClickListener { onRemove(favorite) }
        }
    }

    class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteEntity>() {
        override fun areItemsTheSame(oldItem: FavoriteEntity, newItem: FavoriteEntity): Boolean =
            oldItem.productId == newItem.productId

        override fun areContentsTheSame(oldItem: FavoriteEntity, newItem: FavoriteEntity): Boolean =
            oldItem == newItem
    }
}