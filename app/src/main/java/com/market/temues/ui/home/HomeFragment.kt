package com.market.temues.ui.home

import android.os.Bundle
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
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.market.temues.R
import com.market.temues.data.remote.storage.StorageDataSource
import com.market.temues.databinding.ItemProductCardBinding
import com.market.temues.databinding.PantallaInicioBinding
import com.market.temues.model.Product
import com.market.temues.ui.common.ProductAdapter
import com.market.temues.ui.common.ProductListUiState
import com.market.temues.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: PantallaInicioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter
    private var productosActuales: List<Product> = emptyList()
    private var limiteVisible = PRODUCTOS_POR_CARGA

    @Inject lateinit var storageDataSource: StorageDataSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PantallaInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarListaProductos()
        observarEstado()
        binding.actualizarInicio.setOnRefreshListener { cargarProductosSiHayConexion(forzarRecarga = true) }
        configurarCargaPorScroll()
        cargarProductosSiHayConexion()
    }

    override fun onResume() {
        super.onResume()
        viewModel.actualizarRanking()
    }

    private fun cargarProductosSiHayConexion(forzarRecarga: Boolean = false) {
        if (!NetworkUtils.isOnline(requireContext())) {
            binding.actualizarInicio.isRefreshing = false
            binding.animacionCargaInicio.isVisible = false
            Snackbar.make(binding.root, "Sin conexión a internet", Snackbar.LENGTH_LONG).show()
            return
        }
        viewModel.cargarProductos(forzarRecarga)
    }

    private fun configurarListaProductos() {
        productAdapter = ProductAdapter(
            loadImage = ::loadProductImage,
            onProductClick = { product ->
                findNavController().navigate(
                    R.id.action_home_to_productDetail,
                    Bundle().apply { putString("productId", product.id) }
                )
            }
        )
        binding.listaProductosInicio.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.listaProductosInicio.adapter = productAdapter
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        ProductListUiState.Loading -> mostrarCargando()
                        is ProductListUiState.Success -> {
                            ocultarCargando()
                            renderProducts(state.products)
                        }
                        is ProductListUiState.Empty -> {
                            ocultarCargando()
                            renderProducts(emptyList(), state.message)
                        }
                        is ProductListUiState.Error -> mostrarError(state.message)
                    }
                }
            }
        }
    }


    private fun mostrarCargando() {
        binding.animacionCargaInicio.isVisible = true
        binding.actualizarInicio.isRefreshing = true
    }

    private fun ocultarCargando() {
        binding.animacionCargaInicio.isVisible = false
        binding.actualizarInicio.isRefreshing = false
    }

    private fun mostrarError(mensaje: String) {
        ocultarCargando()
        Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) { cargarProductosSiHayConexion(forzarRecarga = true) }
            .show()
    }

    private fun renderProducts(products: List<Product>, emptyMessage: String = getString(R.string.products_empty_filtered)) {
        productosActuales = products
        limiteVisible = PRODUCTOS_POR_CARGA
        if (products.isEmpty()) {
            Snackbar.make(binding.root, emptyMessage, Snackbar.LENGTH_SHORT).show()
        }
        mostrarProductosVisibles()
    }

    private fun configurarCargaPorScroll() {
        binding.scrollInicio.setOnScrollChangeListener { view, _, scrollY, _, _ ->
            val scrollView = view as androidx.core.widget.NestedScrollView
            val contenido = scrollView.getChildAt(0)?.height ?: return@setOnScrollChangeListener
            val finalVisible = scrollY + scrollView.height
            if (finalVisible >= contenido - UMBRAL_SCROLL_PX) {
                cargarSiguienteBloque()
            }
        }
    }

    private fun cargarSiguienteBloque() {
        if (limiteVisible >= productosActuales.size) return
        limiteVisible = (limiteVisible + PRODUCTOS_POR_CARGA).coerceAtMost(productosActuales.size)
        mostrarProductosVisibles()
    }

    private fun mostrarProductosVisibles() {
        productAdapter.submitList(productosActuales.take(limiteVisible))
    }

    private fun loadProductImage(path: String?, itemBinding: ItemProductCardBinding) {
        if (path.isNullOrBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            storageDataSource.getImageUrl(path).collect { result ->
                result.getOrNull()?.let { imageUrl ->
                    Glide.with(itemBinding.imgProduct)
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_soft_card)
                        .error(R.drawable.bg_soft_card)
                        .into(itemBinding.imgProduct)
                }
            }
        }
    }

    companion object {
        private const val PRODUCTOS_POR_CARGA = 20
        private const val UMBRAL_SCROLL_PX = 240
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
