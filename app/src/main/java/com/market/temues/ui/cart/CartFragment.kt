package com.market.temues.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.market.temues.R
import com.market.temues.databinding.FragmentCartBinding

class CartFragment : Fragment() {
    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnContinueShopping.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
        binding.btnGoPayment.setOnClickListener {
            findNavController().navigate(R.id.action_cart_to_paymentMethod)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
