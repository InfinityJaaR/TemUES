package com.market.temues.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.market.temues.R
import com.market.temues.databinding.FragmentProductDetailBinding

class ProductDetailFragment : Fragment() {
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBuyNow.setOnClickListener {
            findNavController().navigate(R.id.action_productDetail_to_cart)
        }
        binding.btnAddCart.setOnClickListener {
            findNavController().navigate(R.id.action_productDetail_to_cart)
        }
        binding.btnFavorite.setOnClickListener {
            Snackbar.make(binding.root, "Agregado a favoritos", Snackbar.LENGTH_SHORT).show()
        }
        binding.btnMessageSeller.setOnClickListener {
            findNavController().navigate(R.id.chatFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
