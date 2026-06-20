package com.market.temues.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.market.temues.R
import com.market.temues.data.remote.storage.StorageDataSource
import com.market.temues.databinding.PantallaBusquedaBinding
import com.market.temues.model.Product
import com.market.temues.ui.common.ProductListUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: PantallaBusquedaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()

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
        configurarBusquedaYCategorias()
        observarEstado()
        binding.actualizarBusqueda.setOnRefreshListener { viewModel.cargarProductos() }
        viewModel.cargarProductos()
    }

    private fun configurarBusquedaYCategorias() {
        marcarCategoriaSeleccionada(binding.chipBusquedaTodo)
        binding.inputBusquedaProducto.doAfterTextChanged { viewModel.buscar(it?.toString().orEmpty()) }
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
        binding.gridProductosBusqueda.removeAllViews()
        binding.txtEstadoBusqueda.text = if (productos.isEmpty()) {
            emptyMessage
        } else {
            "${productos.size} productos encontrados desde Firestore."
        }

        productos.forEach { producto ->
            val card = layoutInflater.inflate(R.layout.item_product_card, binding.gridProductosBusqueda, false) as LinearLayout
            val productImage = card.findViewById<ImageView>(R.id.img_product)
            card.findViewById<TextView>(R.id.txt_product_name).text = producto.name
            card.findViewById<TextView>(R.id.txt_product_meta).text = "${producto.location} · ${producto.condition}"
            card.findViewById<TextView>(R.id.txt_product_price).text = "$%.2f".format(producto.price)
            loadProductImage(producto.images.firstOrNull(), productImage)
            card.setOnClickListener {
                findNavController().navigate(
                    R.id.action_search_to_productDetail,
                    Bundle().apply { putString("productId", producto.id) }
                )
            }
            card.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            binding.gridProductosBusqueda.addView(card)
        }
    }

    private fun loadProductImage(path: String?, imageView: ImageView) {
        if (path.isNullOrBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            storageDataSource.getImageUrl(path).collect { result ->
                result.getOrNull()?.let { imageUrl ->
                    Glide.with(imageView)
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_soft_card)
                        .error(R.drawable.bg_soft_card)
                        .into(imageView)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
