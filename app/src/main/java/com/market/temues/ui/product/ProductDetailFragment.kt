package com.market.temues.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.market.temues.R
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.data.remote.storage.StorageDataSource
import com.market.temues.databinding.PantallaDetalleProductoBinding
import com.market.temues.model.Product
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {
    private var _binding: PantallaDetalleProductoBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var productRemoteDataSource: ProductRemoteDataSource
    @Inject lateinit var storageDataSource: StorageDataSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PantallaDetalleProductoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productId = arguments?.getString("productId").orEmpty()
        if (productId.isNotBlank()) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    productRemoteDataSource.getById(productId).collect { product ->
                        product?.let { renderProduct(it) }
                    }
                }
            }
        }

        binding.btnBuyNow.setOnClickListener {
            findNavController().navigate(R.id.action_productDetail_to_cart)
        }
        binding.btnAddCart.setOnClickListener {
            findNavController().navigate(R.id.action_productDetail_to_cart)
        }
        binding.btnFavorite.setOnClickListener {
            findNavController().navigate(R.id.action_productDetail_to_favorites)
        }
        binding.btnMessageSeller.setOnClickListener {
            findNavController().navigate(R.id.chatDetailFragment)
        }
    }

    private fun renderProduct(product: Product) {
        binding.txtConditionChip.text = product.condition.replaceFirstChar { it.uppercase() }
        binding.txtLocationChip.text = product.location
        binding.txtProductName.text = product.name
        binding.txtProductPrice.text = formatPrice(product.price)
        binding.txtProductDescription.text = product.description
        binding.txtSeller.text = "Vendedor: ${product.sellerName}"
        binding.txtLocation.text = "Ubicación: ${product.location}"
        binding.txtCondition.text = "Condición: ${product.condition.replaceFirstChar { it.uppercase() }}"
        binding.txtCategory.text = "Categoría: ${product.categoryName}"
        loadProductImage(product.images.firstOrNull())
    }

    private fun loadProductImage(path: String?) {
        if (path.isNullOrBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            storageDataSource.getImageUrl(path).collect { result ->
                result.getOrNull()?.let { imageUrl ->
                    Glide.with(binding.imgProductDetail)
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_temues_gradient)
                        .error(R.drawable.bg_temues_gradient)
                        .into(binding.imgProductDetail)
                }
            }
        }
    }

    private fun formatPrice(price: Double) = "$%.2f".format(price)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
