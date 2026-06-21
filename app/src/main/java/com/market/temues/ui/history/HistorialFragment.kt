package com.market.temues.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.market.temues.databinding.PantallaHistorialComprasBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistorialFragment : Fragment() {

    private var _binding: PantallaHistorialComprasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistorialViewModel by viewModels()
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
        configurarRecyclerView()
        observarEstado()
    }

    private fun configurarRecyclerView() {
        adaptador = OrdenAdapter()
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
                is HistorialViewModel.HistorialUiState.Cargando -> {
                    binding.progressHistorial.visibility = View.VISIBLE
                }
                is HistorialViewModel.HistorialUiState.Exitoso -> {
                    binding.rvOrdenes.visibility = View.VISIBLE
                    adaptador.submitList(estado.ordenes)
                }
                is HistorialViewModel.HistorialUiState.Vacio -> {
                    binding.layoutVacioHistorial.visibility = View.VISIBLE
                }
                is HistorialViewModel.HistorialUiState.Error -> {
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
