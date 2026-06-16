package com.market.temues.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.market.temues.R
import com.market.temues.databinding.FragmentCardPaymentBinding

class CardPaymentFragment : Fragment() {
    private var _binding: FragmentCardPaymentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnReviewPurchase.setOnClickListener {
            findNavController().navigate(R.id.action_cardPayment_to_confirmPurchase)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
