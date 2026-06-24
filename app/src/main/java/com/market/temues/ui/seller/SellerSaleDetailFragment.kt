package com.market.temues.ui.seller

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.market.temues.R
import com.market.temues.databinding.PantallaDetalleVentaVendedorBinding
import com.market.temues.model.Orden
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SellerSaleDetailFragment : Fragment() {

    private var _binding: PantallaDetalleVentaVendedorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SellerSaleDetailViewModel by viewModels()

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
        observarEstado()
        binding.btnContactBuyer.setOnClickListener {
            findNavController().navigate(R.id.chatFragment)
        }
    }

    private fun observarEstado() {
        viewModel.uiState.asLiveData().observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is SellerSaleDetailViewModel.SellerSaleDetailUiState.Cargando -> Unit
                is SellerSaleDetailViewModel.SellerSaleDetailUiState.Exitoso -> {
                    mostrarVenta(estado.orden)
                }
                is SellerSaleDetailViewModel.SellerSaleDetailUiState.Error -> {
                    Snackbar.make(binding.root, estado.mensaje, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarVenta(orden: Orden) {
        val productos = orden.articulos.joinToString(", ") { it.nombreProducto }
        binding.txtProductoVenta.text = "Producto vendido: ${productos.ifBlank { "—" }}"
        binding.txtCompradorVenta.text = "Comprador: ${orden.usuarioId}"
        binding.txtTotalVenta.text = "Total: $%.2f".format(orden.total)

        val (textoEstado, colorEstado) = when (orden.estado) {
            "confirmado" -> "Confirmado" to Color.parseColor("#1565C0")
            "entregado"  -> "Entregado"  to Color.parseColor("#2E7D32")
            "cancelado"  -> "Cancelado"  to Color.parseColor("#C62828")
            else         -> "Pendiente"  to Color.parseColor("#E65100")
        }
        binding.txtEstadoVenta.text = "Estado: $textoEstado"
        binding.txtEstadoVenta.setTextColor(colorEstado)

        binding.btnValidateDelivery.setOnClickListener {
            val codigo = binding.etBuyerCode.text?.toString().orEmpty().trim()
            if (codigo.isBlank()) {
                Snackbar.make(binding.root, "Ingresa el código del comprador", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (codigo == orden.codigo) {
                viewModel.marcarEntregada()
                Snackbar.make(binding.root, "Entrega validada. Pago liberado.", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(binding.root, "Código incorrecto", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
