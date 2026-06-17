package com.market.temues.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: PantallaInicioBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var productRemoteDataSource: ProductRemoteDataSource
    @Inject lateinit var firestoreSeeder: FirestoreSeeder
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

        viewLifecycleOwner.lifecycleScope.launch {
            firestoreSeeder.seed()

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                productRemoteDataSource.getAll().collect { products ->
                    renderProducts(products)
                }
            }
        }
    }

    private fun renderProducts(products: List<Product>) {
        binding.gridProducts.removeAllViews()
        binding.txtProductsStatus.text = if (products.isEmpty()) {
            "No hay productos disponibles. Carga los datos desde el seeder."
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
