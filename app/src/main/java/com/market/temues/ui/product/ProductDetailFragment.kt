package com.market.temues.ui.product

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
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
import com.market.temues.model.User
import com.market.temues.ui.cart.CarritoViewModel
import com.market.temues.ui.common.ProductDetailUiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {
    private var _binding: PantallaDetalleProductoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductDetailViewModel by viewModels()
    private val carritoViewModel: CarritoViewModel by viewModels()

    private var esFavoritoSeleccionado = false
    private var productoActual: com.market.temues.model.Product? = null

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
        observarVendedor()
        observarEstadoFavorito()
        observarEventos()

        binding.btnBuyNow.setOnClickListener { viewModel.onBuyNowClicked() }
        binding.btnAddCart.setOnClickListener { viewModel.onAddToCartClicked() }
        binding.btnFavorite.setOnClickListener { viewModel.onFavoriteClicked() }
        binding.btnMessageSeller.setOnClickListener { viewModel.onChatClicked() }
    }

    private fun observarEventos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ProductDetailEvent.ToggleFavorite -> {
                            val mensaje = if (event.isFavorite) {
                                getString(R.string.favorite_added)
                            } else {
                                getString(R.string.favorite_removed)
                            }
                            mostrarMensajeFavorito(mensaje, event.isFavorite)
                        }
                        is ProductDetailEvent.OpenChat -> {
                            findNavController().navigate(
                                R.id.action_productDetail_to_chatDetail,
                                Bundle().apply {
                                    putString("sellerId", event.sellerId)
                                    putString("productId", event.productId)
                                }
                            )
                        }
                        is ProductDetailEvent.AddToCart -> {
                            productoActual?.let { carritoViewModel.agregarAlCarrito(it) }
                            findNavController().navigate(R.id.action_productDetail_to_cart)
                        }
                        is ProductDetailEvent.BuyNow -> {
                            productoActual?.let { carritoViewModel.agregarAlCarrito(it) }
                            findNavController().navigate(
                                R.id.action_productDetail_to_checkout,
                                Bundle().apply { putString("productId", event.productId) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun mostrarMensajeFavorito(mensaje: String, agregado: Boolean) {
        val contenedor = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 18, 28, 18)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 42f
                setColor(requireContext().getColor(if (agregado) R.color.temues_green else R.color.temues_navy))
            }
            elevation = 10f
        }

        val texto = TextView(requireContext()).apply {
            text = "${if (agregado) "❤" else "♡"}  $mensaje"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }

        contenedor.addView(texto)

        Toast(requireContext()).apply {
            duration = Toast.LENGTH_SHORT
            view = contenedor
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 120)
            show()
        }
    }

    private fun observarVendedor() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.seller.collect { seller ->
                    renderSeller(seller)
                }
            }
        }
    }

    private fun renderSeller(seller: User?) {
        if (seller == null) return

        binding.txtSeller.text = seller.name.ifBlank { productoActual?.sellerName ?: getString(R.string.seller_without_name) }
        binding.txtSellerPhone.isVisible = seller.phone.isNotBlank()
        binding.txtSellerPhone.text = getString(R.string.seller_phone_format, seller.phone)
        binding.txtSellerBio.isVisible = seller.bio.isNotBlank()
        binding.txtSellerBio.text = seller.bio
        loadSellerPhoto(seller.photoUrl)
    }

    private fun observarEstadoFavorito() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.esFavorito.collect { favorito ->
                    esFavoritoSeleccionado = favorito
                    val colorFavorito = requireContext().getColor(
                        if (favorito) R.color.temues_red else R.color.temues_text_muted
                    )
                    binding.btnFavorite.iconTint = android.content.res.ColorStateList.valueOf(colorFavorito)
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

    private fun renderProduct(product: Product) {
        productoActual = product
        val lugarEntrega = product.location.ifBlank { getString(R.string.checkout_coordinar_chat) }
        binding.txtProductName.text = product.name
        binding.txtProductPrice.text = formatPrice(product.price)
        binding.txtProductDescription.text = product.description
        binding.txtSeller.text = product.sellerName.ifBlank { getString(R.string.seller_without_name) }
        binding.txtSellerPhone.isVisible = false
        binding.txtSellerBio.isVisible = false
        binding.imgSellerPhoto.isVisible = false
        binding.txtLocation.text = getString(R.string.product_place_format, lugarEntrega)
        binding.txtCondition.text = getString(R.string.product_condition_format, product.condition.replaceFirstChar { it.uppercase() })
        binding.txtDeliveryPoint.text = getString(R.string.product_delivery_point_format, lugarEntrega)
        binding.txtCategory.text = getString(R.string.product_category_format, product.categoryName)
        binding.imgSellerPhoto.setImageResource(R.drawable.bg_temues_gradient)
        loadProductImage(product.images.firstOrNull())
    }

    private fun loadSellerPhoto(path: String?) {
        if (path.isNullOrBlank()) {
            binding.imgSellerPhoto.isVisible = false
            return
        }

        binding.imgSellerPhoto.isVisible = true

        if (path.startsWith("http://") || path.startsWith("https://")) {
            Glide.with(binding.imgSellerPhoto)
                .load(path)
                .placeholder(R.drawable.bg_temues_gradient)
                .error(R.drawable.bg_temues_gradient)
                .circleCrop()
                .into(binding.imgSellerPhoto)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            storageDataSource.getImageUrl(path).collect { result ->
                result.getOrNull()?.let { imageUrl ->
                    Glide.with(binding.imgSellerPhoto)
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_temues_gradient)
                        .error(R.drawable.bg_temues_gradient)
                        .circleCrop()
                        .into(binding.imgSellerPhoto)
                } ?: run {
                    binding.imgSellerPhoto.isVisible = false
                }
            }
        }
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
