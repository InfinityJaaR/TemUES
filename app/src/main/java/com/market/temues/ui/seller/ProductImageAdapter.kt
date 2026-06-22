package com.market.temues.ui.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.market.temues.databinding.ItemProductImageBinding

class ProductImageAdapter(
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<ProductImageAdapter.ViewHolder>() {

    private var imagenes = listOf<String>()

    fun submitList(list: List<String>) {
        imagenes = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.vincular(imagenes[position], position)
    }

    override fun getItemCount() = imagenes.size

    inner class ViewHolder(private val binding: ItemProductImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(url: String, index: Int) {
            Glide.with(binding.ivImage)
                .load(url)
                .into(binding.ivImage)
            binding.btnRemove.setOnClickListener { onRemove(index) }
        }
    }
}
