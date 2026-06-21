package com.market.temues.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.market.temues.R
import com.market.temues.databinding.PantallaCarritoBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CartFragment : Fragment() {

    private var _binding: PantallaCarritoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarritoViewModel by viewModels()
    private lateinit var adaptador: CarritoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PantallaCarritoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarRecyclerView()
        observarEstado()
        configurarBotones()
    }

    private fun configurarRecyclerView() {
        adaptador = CarritoAdapter(
            onAumentar = { productoId -> viewModel.aumentarCantidad(productoId) },
            onDisminuir = { productoId -> viewModel.disminuirCantidad(productoId) },
            onEliminar = { productoId -> viewModel.eliminarDelCarrito(productoId) }
        )
        binding.rvArticulos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adaptador
        }
    }

    private fun observarEstado() {
        viewModel.articulos.asLiveData().observe(viewLifecycleOwner) { articulos ->
            adaptador.submitList(articulos)
            val carritoVacio = articulos.isEmpty()
            binding.rvArticulos.visibility = if (carritoVacio) View.GONE else View.VISIBLE
            binding.layoutVacio.visibility = if (carritoVacio) View.VISIBLE else View.GONE
            binding.layoutFooter.visibility = if (carritoVacio) View.GONE else View.VISIBLE
        }

        viewModel.total.asLiveData().observe(viewLifecycleOwner) { total ->
            binding.txtTotal.text = "$%.2f".format(total)
        }
    }

    private fun configurarBotones() {
        binding.btnExplorar.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
        binding.btnIrAPagar.setOnClickListener {
            findNavController().navigate(R.id.action_cart_to_checkout)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
