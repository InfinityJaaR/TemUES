package com.market.temues.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.market.temues.R
import com.market.temues.databinding.PantallaHistorialComprasBinding
import com.market.temues.ui.history.OrdenAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistorialVentasFragment : Fragment() {

    private var _binding: PantallaHistorialComprasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistorialVentasViewModel by viewModels()
    private lateinit var adaptador: OrdenAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PantallaHistorialComprasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.txtHistorialVacio.setText(R.string.ventas_vacio)
        configurarRecyclerView()
        observarEstado()
    }

    private fun configurarRecyclerView() {
        adaptador = OrdenAdapter { orden ->
            val argumentos = Bundle().apply { putString("ordenId", orden.id) }
            findNavController().navigate(R.id.action_historialVentas_to_sellerSaleDetail, argumentos)
        }
        binding.rvOrdenes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adaptador
        }
    }

    private fun observarEstado() {
        viewModel.uiState.asLiveData().observe(viewLifecycleOwner) { estado ->
            binding.progressHistorial.visibility = View.GONE
            binding.rvOrdenes.visibility = View.GONE
            binding.layoutVacioHistorial.visibility = View.GONE

            when (estado) {
                is HistorialVentasViewModel.HistorialVentasUiState.Cargando -> {
                    binding.progressHistorial.visibility = View.VISIBLE
                }
                is HistorialVentasViewModel.HistorialVentasUiState.Exitoso -> {
                    binding.rvOrdenes.visibility = View.VISIBLE
                    adaptador.submitList(estado.ordenes)
                }
                is HistorialVentasViewModel.HistorialVentasUiState.Vacio -> {
                    binding.layoutVacioHistorial.visibility = View.VISIBLE
                }
                is HistorialVentasViewModel.HistorialVentasUiState.Error -> {
                    Snackbar.make(binding.root, estado.mensaje, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
