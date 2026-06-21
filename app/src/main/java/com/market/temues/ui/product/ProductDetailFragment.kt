package com.market.temues.ui.product

import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.market.temues.R
import com.market.temues.data.remote.storage.StorageDataSource
import com.market.temues.databinding.PantallaDetalleProductoBinding
import com.market.temues.model.Product
import com.market.temues.ui.common.ProductDetailUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {
    private var _binding: PantallaDetalleProductoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductDetailViewModel by viewModels()

    private var esFavoritoSeleccionado = false

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
        observarProducto()
        observarEventos()
        binding.btnBuyNow.setOnClickListener { viewModel.comprarAhora() }
        binding.btnAddCart.setOnClickListener { viewModel.agregarAlCarrito() }
        binding.btnFavorite.setOnClickListener { viewModel.alternarFavorito() }
        binding.btnMessageSeller.setOnClickListener { viewModel.abrirChat() }
    }

    private fun observarEventos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventos.collect { evento ->
                    when (evento) {
                        is ProductDetailEvent.ToggleFavorite -> alternarFavoritoVisual()
                        is ProductDetailEvent.OpenChat -> findNavController().navigate(
                            R.id.chatDetailFragment,
                            Bundle().apply {
                                putString("sellerId", evento.sellerId)
                                putString("productId", evento.productId)
                            }
                        )
                        is ProductDetailEvent.AddToCart -> findNavController().navigate(
                            R.id.action_productDetail_to_cart,
                            Bundle().apply { putString("productId", evento.productId) }
                        )
                        is ProductDetailEvent.BuyNow -> findNavController().navigate(
                            R.id.action_productDetail_to_confirmPurchase,
                            Bundle().apply { putString("productId", evento.productId) }
                        )
                    }
                }
            }
        }
    }

    private fun observarProducto() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        ProductDetailUiState.Loading -> mostrarEstadoDetalle("Cargando producto...")
                        is ProductDetailUiState.Success -> {
                            binding.txtProductDescription.isVisible = true
                            renderProduct(state.product)
                        }
                        is ProductDetailUiState.Empty -> mostrarEstadoDetalle(state.message)
                        is ProductDetailUiState.Error -> mostrarEstadoDetalle(state.message)
                    }
                }
            }
        }
    }

    private fun mostrarEstadoDetalle(mensaje: String) {
        binding.txtProductName.text = mensaje
        binding.txtProductPrice.text = ""
        binding.txtProductDescription.isVisible = false
    }

    private fun alternarFavoritoVisual() {
        esFavoritoSeleccionado = !esFavoritoSeleccionado
        val colorFavorito = requireContext().getColor(
            if (esFavoritoSeleccionado) R.color.temues_red else R.color.temues_text_muted
        )
        binding.btnFavorite.iconTint = android.content.res.ColorStateList.valueOf(colorFavorito)
        val mensaje = if (esFavoritoSeleccionado) getString(R.string.favorite_added) else getString(R.string.favorite_removed)
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun renderProduct(product: Product) {
        val lugarEntrega = product.location.ifBlank { getString(R.string.coordinate_by_chat) }
        binding.txtProductName.text = product.name
        binding.txtProductPrice.text = formatPrice(product.price)
        binding.txtProductDescription.text = product.description
        binding.txtSeller.text = product.sellerName.ifBlank { getString(R.string.seller_without_name) }
        binding.txtLocation.text = getString(R.string.delivery_place_format, lugarEntrega)
        binding.txtCondition.text = getString(R.string.condition_format, product.condition.replaceFirstChar { it.uppercase() })
        binding.txtDeliveryPoint.text = getString(R.string.delivery_point_format, lugarEntrega)
        binding.txtCategory.text = getString(R.string.category_format, product.categoryName)
        binding.imgSellerPhoto.setImageResource(R.drawable.bg_temues_gradient)
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

    private fun formatPrice(price: Double) = getString(R.string.product_price_format, price)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
