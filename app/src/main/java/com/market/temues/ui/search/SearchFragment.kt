package com.market.temues.ui.search

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
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
import com.market.temues.model.Category
import com.market.temues.model.Product
import com.market.temues.ui.common.ProductAdapter
import com.market.temues.ui.common.ProductListUiState
import com.market.temues.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: PantallaBusquedaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter
    private var categoriasFirebase: List<Category> = emptyList()

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
        observarCategorias()
        binding.actualizarBusqueda.setOnRefreshListener { cargarProductosSiHayConexion() }
        cargarProductosSiHayConexion()
    }

    private fun cargarProductosSiHayConexion() {
        if (!NetworkUtils.isOnline(requireContext())) {
            binding.actualizarBusqueda.isRefreshing = false
            binding.animacionCargaBusqueda.isVisible = false
            Snackbar.make(binding.root, "Sin conexión a internet", Snackbar.LENGTH_LONG).show()
            return
        }
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
                if (NetworkUtils.isOnline(requireContext())) {
                    viewModel.buscar(textView.text?.toString().orEmpty())
                } else {
                    Snackbar.make(binding.root, "Sin conexión a internet", Snackbar.LENGTH_LONG).show()
                }
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
        binding.chipBusquedaServicios.setOnClickListener { mostrarMenuCategorias(binding.chipBusquedaServicios) }
    }

    private fun observarCategorias() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categorias ->
                    categoriasFirebase = categorias
                }
            }
        }
    }

    private fun mostrarMenuCategorias(anchor: TextView) {
        val categoriasMenu = categoriasFirebase
            .filter { it.id.isNotBlank() }
            .sortedBy { it.order }

        if (categoriasMenu.isEmpty()) {
            seleccionarCategoria("otros", binding.chipBusquedaServicios)
            return
        }

        val popup = PopupMenu(requireContext(), anchor)
        categoriasMenu.forEachIndexed { index, category ->
            popup.menu.add(0, index, index, category.name.ifBlank { category.id })
        }
        popup.setOnMenuItemClickListener { item ->
            categoriasMenu.getOrNull(item.itemId)?.let { category ->
                if (category.id.equals("otros", ignoreCase = true)) {
                    seleccionarCategoria("", binding.chipBusquedaTodo)
                    binding.chipBusquedaServicios.text = getString(R.string.home_categories_more)
                } else {
                    seleccionarCategoria(category.id, binding.chipBusquedaServicios)
                    binding.chipBusquedaServicios.text = category.name.ifBlank { category.id }
                }
            }
            true
        }
        popup.show()
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
        binding.txtEstadoBusqueda.text = getString(R.string.products_loading_firestore)
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
            .setAction(R.string.retry) { cargarProductosSiHayConexion() }
            .show()
    }

    private fun seleccionarCategoria(categoriaId: String, chipSeleccionado: TextView) {
        if (!NetworkUtils.isOnline(requireContext())) {
            Snackbar.make(binding.root, "Sin conexión a internet", Snackbar.LENGTH_LONG).show()
            return
        }
        marcarCategoriaSeleccionada(chipSeleccionado)
        if (chipSeleccionado != binding.chipBusquedaServicios) {
            binding.chipBusquedaServicios.text = getString(R.string.home_categories_more)
        }
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

    private fun renderProductos(productos: List<Product>, emptyMessage: String = getString(R.string.products_empty_filtered)) {
        binding.txtEstadoBusqueda.text = if (productos.isEmpty()) {
            emptyMessage
        } else {
            getString(R.string.products_found_count, productos.size)
        }
        ajustarAlturaLista(productos.size)
        productAdapter.submitList(productos)
    }

    private fun ajustarAlturaLista(cantidadProductos: Int) {
        val filas = ((cantidadProductos + 1) / 2).coerceAtLeast(1)
        val alturaPorFila = (330 * resources.displayMetrics.density).toInt()
        binding.listaProductosBusqueda.layoutParams = binding.listaProductosBusqueda.layoutParams.apply {
            height = filas * alturaPorFila
        }
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
