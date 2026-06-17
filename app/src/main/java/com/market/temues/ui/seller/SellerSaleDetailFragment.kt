package com.market.temues.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.market.temues.R
import com.market.temues.databinding.PantallaDetalleVentaVendedorBinding

class SellerSaleDetailFragment : Fragment() {
    private var _binding: PantallaDetalleVentaVendedorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PantallaDetalleVentaVendedorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnValidateDelivery.setOnClickListener {
            Snackbar.make(binding.root, "Entrega validada y pago listo para liberar", Snackbar.LENGTH_LONG).show()
        }
        binding.btnContactBuyer.setOnClickListener {
            findNavController().navigate(R.id.chatFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
