package com.market.temues.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: PantallaInicioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter

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
        configurarBusquedaYCategorias()
        observarEstado()
        binding.actualizarInicio.setOnRefreshListener { viewModel.cargarProductos(forzarRecarga = true) }
        viewModel.cargarProductos()
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

    private fun configurarBusquedaYCategorias() {
        marcarCategoriaSeleccionada(binding.chipAll)
        binding.inputProductSearch.doAfterTextChanged { viewModel.buscar(it?.toString().orEmpty()) }
        binding.chipAll.setOnClickListener { seleccionarCategoria("", binding.chipAll) }
        binding.chipElectronics.setOnClickListener { seleccionarCategoria("electronica", binding.chipElectronics) }
        binding.chipClothes.setOnClickListener { seleccionarCategoria("ropa", binding.chipClothes) }
        binding.chipHome.setOnClickListener { seleccionarCategoria("hogar", binding.chipHome) }
        binding.chipServices.setOnClickListener { seleccionarCategoria("servicios", binding.chipServices) }
    }

    private fun seleccionarCategoria(categoriaId: String, chipSeleccionado: TextView) {
        marcarCategoriaSeleccionada(chipSeleccionado)
        viewModel.seleccionarCategoria(categoriaId)
    }

    private fun marcarCategoriaSeleccionada(chipSeleccionado: TextView) {
        val chips = listOf(binding.chipAll, binding.chipElectronics, binding.chipClothes, binding.chipHome, binding.chipServices)
        chips.forEach { chip ->
            val estaSeleccionado = chip == chipSeleccionado
            chip.setBackgroundResource(if (estaSeleccionado) R.drawable.bg_chip_selected else R.drawable.bg_chip)
            chip.setTextColor(requireContext().getColor(if (estaSeleccionado) R.color.white else R.color.temues_navy))
        }
    }

    private fun mostrarCargando() {
        binding.animacionCargaInicio.isVisible = true
        binding.actualizarInicio.isRefreshing = true
        binding.txtProductsStatus.text = "Cargando productos desde Firestore..."
    }

    private fun ocultarCargando() {
        binding.animacionCargaInicio.isVisible = false
        binding.actualizarInicio.isRefreshing = false
    }

    private fun mostrarError(mensaje: String) {
        ocultarCargando()
        binding.txtProductsStatus.text = mensaje
        Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") { viewModel.cargarProductos(forzarRecarga = true) }
            .show()
    }

    private fun renderProducts(products: List<Product>, emptyMessage: String = "No hay productos que coincidan con tu búsqueda o categoría.") {
        binding.txtProductsStatus.text = if (products.isEmpty()) {
            emptyMessage
        } else {
            "${products.size} productos cargados desde Firestore."
        }
        productAdapter.submitList(products)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
