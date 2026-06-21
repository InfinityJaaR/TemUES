package com.market.temues.ui.search

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
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
import com.market.temues.databinding.PantallaBusquedaBinding
import com.market.temues.model.Product
import com.market.temues.ui.common.ProductAdapter
import com.market.temues.ui.common.ProductListUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: PantallaBusquedaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter

    @Inject lateinit var storageDataSource: StorageDataSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PantallaBusquedaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarListaProductos()
        configurarBusquedaYCategorias()
        observarEstado()
        binding.actualizarBusqueda.setOnRefreshListener { viewModel.cargarProductos() }
        viewModel.cargarProductos()
    }

    private fun configurarListaProductos() {
        productAdapter = ProductAdapter(
            loadImage = ::loadProductImage,
            onProductClick = { producto ->
                findNavController().navigate(
                    R.id.action_search_to_productDetail,
                    Bundle().apply { putString("productId", producto.id) }
                )
            }
        )
        binding.listaProductosBusqueda.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.listaProductosBusqueda.adapter = productAdapter
    }

    private fun configurarBusquedaYCategorias() {
        marcarCategoriaSeleccionada(binding.chipBusquedaTodo)
        binding.inputBusquedaProducto.setOnEditorActionListener { textView, actionId, event ->
            val esAccionBuscar = actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
            val esEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (esAccionBuscar || esEnter) {
                viewModel.buscar(textView.text?.toString().orEmpty())
                binding.inputBusquedaProducto.clearFocus()
                true
            } else {
                false
            }
        }
        binding.chipBusquedaTodo.setOnClickListener { seleccionarCategoria("", binding.chipBusquedaTodo) }
        binding.chipBusquedaElectronica.setOnClickListener { seleccionarCategoria("electronica", binding.chipBusquedaElectronica) }
        binding.chipBusquedaRopa.setOnClickListener { seleccionarCategoria("ropa", binding.chipBusquedaRopa) }
        binding.chipBusquedaHogar.setOnClickListener { seleccionarCategoria("hogar", binding.chipBusquedaHogar) }
        binding.chipBusquedaServicios.setOnClickListener { seleccionarCategoria("servicios", binding.chipBusquedaServicios) }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        ProductListUiState.Loading -> mostrarCargando()
                        is ProductListUiState.Success -> {
                            ocultarCargando()
                            renderProductos(state.products)
                        }
                        is ProductListUiState.Empty -> {
                            ocultarCargando()
                            renderProductos(emptyList(), state.message)
                        }
                        is ProductListUiState.Error -> mostrarError(state.message)
                    }
                }
            }
        }
    }

    private fun mostrarCargando() {
        binding.animacionCargaBusqueda.isVisible = true
        binding.actualizarBusqueda.isRefreshing = true
        binding.txtEstadoBusqueda.text = "Cargando productos desde Firestore..."
    }

    private fun ocultarCargando() {
        binding.animacionCargaBusqueda.isVisible = false
        binding.actualizarBusqueda.isRefreshing = false
    }

    private fun mostrarError(mensaje: String) {
        binding.animacionCargaBusqueda.isVisible = false
        binding.actualizarBusqueda.isRefreshing = false
        binding.txtEstadoBusqueda.text = mensaje
        Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") { viewModel.cargarProductos() }
            .show()
    }

    private fun seleccionarCategoria(categoriaId: String, chipSeleccionado: TextView) {
        marcarCategoriaSeleccionada(chipSeleccionado)
        viewModel.seleccionarCategoria(categoriaId)
    }

    private fun marcarCategoriaSeleccionada(chipSeleccionado: TextView) {
        val chips = listOf(
            binding.chipBusquedaTodo,
            binding.chipBusquedaElectronica,
            binding.chipBusquedaRopa,
            binding.chipBusquedaHogar,
            binding.chipBusquedaServicios
        )
        chips.forEach { chip ->
            val estaSeleccionado = chip == chipSeleccionado
            chip.setBackgroundResource(if (estaSeleccionado) R.drawable.bg_chip_selected else R.drawable.bg_chip)
            chip.setTextColor(requireContext().getColor(if (estaSeleccionado) R.color.white else R.color.temues_navy))
        }
    }

    private fun renderProductos(productos: List<Product>, emptyMessage: String = "No hay productos que coincidan con tu búsqueda o categoría.") {
        binding.txtEstadoBusqueda.text = if (productos.isEmpty()) {
            emptyMessage
        } else {
            "${productos.size} productos encontrados desde Firestore."
        }
        productAdapter.submitList(productos)
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
