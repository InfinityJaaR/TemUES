package com.market.temues.ui.history

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.market.temues.databinding.PantallaDetalleOrdenBinding
import com.market.temues.model.Orden
import com.market.temues.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetalleOrdenFragment : Fragment() {

    private var _binding: PantallaDetalleOrdenBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetalleOrdenViewModel by viewModels()
    private lateinit var adaptadorArticulos: ArticuloOrdenAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PantallaDetalleOrdenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarRecyclerView()
        observarEstado()
    }

    private fun configurarRecyclerView() {
        adaptadorArticulos = ArticuloOrdenAdapter()
        binding.rvArticulosDetalle.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adaptadorArticulos
        }
    }

    private fun observarEstado() {
        viewModel.uiState.asLiveData().observe(viewLifecycleOwner) { estado ->
            binding.progressDetalle.visibility = View.GONE
            binding.scrollDetalle.visibility = View.GONE
            binding.txtErrorDetalle.visibility = View.GONE

            when (estado) {
                is DetalleOrdenViewModel.DetalleOrdenUiState.Cargando -> {
                    binding.progressDetalle.visibility = View.VISIBLE
                }
                is DetalleOrdenViewModel.DetalleOrdenUiState.Exitoso -> {
                    binding.scrollDetalle.visibility = View.VISIBLE
                    mostrarOrden(estado.orden)
                }
                is DetalleOrdenViewModel.DetalleOrdenUiState.Error -> {
                    binding.txtErrorDetalle.visibility = View.VISIBLE
                    binding.txtErrorDetalle.text = estado.mensaje
                }
            }
        }
    }

    private fun mostrarOrden(orden: Orden) {
        binding.txtCodigoDetalle.text = orden.codigo
        binding.txtFechaDetalle.text = DateUtils.formatTimestamp(orden.creadoEn)
        binding.txtTotalDetalle.text = "$%.2f".format(orden.total)

        val lugar = orden.lugarEntrega.ifBlank { "Coordinar por chat" }
        binding.txtLugarDetalle.text = "Punto de entrega: $lugar"

        val metodo = if (orden.metodoPago == "tarjeta") "Tarjeta" else "Efectivo"
        binding.txtMetodoDetalle.text = "Método de pago: $metodo"

        val (textoEstado, colorEstado) = when (orden.estado) {
            "confirmado" -> "Confirmado" to Color.parseColor("#1565C0")
            "entregado"  -> "Entregado"  to Color.parseColor("#2E7D32")
            "cancelado"  -> "Cancelado"  to Color.parseColor("#C62828")
            else         -> "Pendiente"  to Color.parseColor("#E65100")
        }
        binding.txtEstadoDetalle.text = textoEstado
        binding.txtEstadoDetalle.setTextColor(colorEstado)

        adaptadorArticulos.submitList(orden.articulos)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
