package com.market.temues.ui.home

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.market.temues.R
import com.market.temues.data.remote.FirestoreSeeder
import com.market.temues.data.remote.product.ProductRemoteDataSource
import com.market.temues.data.remote.storage.StorageDataSource
import com.market.temues.databinding.PantallaInicioBinding
import com.market.temues.model.Product
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: PantallaInicioBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var productRemoteDataSource: ProductRemoteDataSource
    @Inject lateinit var firestoreSeeder: FirestoreSeeder
    @Inject lateinit var storageDataSource: StorageDataSource

    private var productosFirestore: List<Product> = emptyList()
    private var categoriaSeleccionada: String = ""

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
        configurarBusquedaYCategorias()

        viewLifecycleOwner.lifecycleScope.launch {
            mostrarCargando()
            firestoreSeeder.seed()

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                productRemoteDataSource.getAll()
                    .catch { error -> mostrarError(error.message ?: "No se pudieron cargar los productos.") }
                    .collect { products ->
                        productosFirestore = products
                        ocultarCargando()
                        aplicarFiltros()
                    }
            }
        }
    }

    private fun configurarBusquedaYCategorias() {
        marcarCategoriaSeleccionada(binding.chipAll)
        binding.inputProductSearch.doAfterTextChanged { aplicarFiltros() }
        binding.chipAll.setOnClickListener { seleccionarCategoria("", binding.chipAll) }
        binding.chipElectronics.setOnClickListener { seleccionarCategoria("electronica", binding.chipElectronics) }
        binding.chipClothes.setOnClickListener { seleccionarCategoria("ropa", binding.chipClothes) }
        binding.chipHome.setOnClickListener { seleccionarCategoria("hogar", binding.chipHome) }
        binding.chipServices.setOnClickListener { seleccionarCategoria("servicios", binding.chipServices) }
    }

    private fun seleccionarCategoria(categoriaId: String, chipSeleccionado: TextView) {
        categoriaSeleccionada = categoriaId
        marcarCategoriaSeleccionada(chipSeleccionado)
        aplicarFiltros()
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
        binding.txtProductsStatus.text = "Cargando productos desde Firestore..."
    }

    private fun ocultarCargando() {
        binding.animacionCargaInicio.isVisible = false
    }

    private fun mostrarError(mensaje: String) {
        binding.animacionCargaInicio.isVisible = false
        binding.txtProductsStatus.text = mensaje
    }

    private fun aplicarFiltros() {
        val textoBusqueda = binding.inputProductSearch.text?.toString().orEmpty().trim().lowercase()
        val productosFiltrados = productosFirestore.filter { producto ->
            val coincideTexto = textoBusqueda.isBlank() || producto.coincideConBusqueda(textoBusqueda)
            val coincideCategoria = categoriaSeleccionada.isBlank() || producto.categoryId == categoriaSeleccionada
            coincideTexto && coincideCategoria
        }
        renderProducts(productosFiltrados)
    }

    private fun Product.coincideConBusqueda(textoBusqueda: String): Boolean {
        val textoProducto = listOf(
            name,
            description,
            price.toString(),
            categoryId,
            categoryName,
            sellerName,
            condition,
            location,
            tags.joinToString(" ")
        ).joinToString(" ").lowercase()
        return textoProducto.contains(textoBusqueda)
    }

    private fun renderProducts(products: List<Product>) {
        binding.gridProducts.removeAllViews()
        binding.txtProductsStatus.text = if (products.isEmpty()) {
            "No hay productos que coincidan con tu búsqueda o categoría."
        } else {
            "${products.size} productos cargados desde Firestore."
        }

        products.forEach { product ->
            val card = layoutInflater.inflate(R.layout.item_product_card, binding.gridProducts, false) as LinearLayout
            val productImage = card.findViewById<ImageView>(R.id.img_product)
            card.findViewById<TextView>(R.id.txt_product_name).text = product.name
            card.findViewById<TextView>(R.id.txt_product_meta).text = "${product.location} · ${product.condition}"
            card.findViewById<TextView>(R.id.txt_product_price).text = formatPrice(product.price)
            loadProductImage(product.images.firstOrNull(), productImage)
            card.setOnClickListener {
                findNavController().navigate(
                    R.id.action_home_to_productDetail,
                    Bundle().apply { putString("productId", product.id) }
                )
            }
            card.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            binding.gridProducts.addView(card)
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

    private fun formatPrice(price: Double) = "$%.2f".format(price)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
