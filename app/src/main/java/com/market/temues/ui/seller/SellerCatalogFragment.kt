package com.market.temues.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.market.temues.R
import com.market.temues.databinding.FragmentSellerCatalogBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SellerCatalogFragment : Fragment() {

    private var _binding: FragmentSellerCatalogBinding? = null
    private val binding get() = _binding!!

    private val modelo: SellerCatalogViewModel by viewModels()
    private lateinit var adaptador: SellerProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellerCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarLista()
        configurarBotonFlotante()
        observarEstado()
    }

    private fun configurarLista() {
        adaptador = SellerProductAdapter(
            alEditar = { producto ->
                val accion = SellerCatalogFragmentDirections.actionSellerCatalogToAddEditProduct(producto.id)
                findNavController().navigate(accion)
            },
            alEliminar = { producto ->
                mostrarConfirmacionEliminar(producto.id)
            },
            alCambiarEstado = { producto ->
                modelo.cambiarEstado(producto.id, producto.status)
            }
        )
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SellerCatalogFragment.adaptador
        }
    }

    private fun configurarBotonFlotante() {
        binding.fabAddProduct.setOnClickListener {
            val accion = SellerCatalogFragmentDirections.actionSellerCatalogToAddEditProduct("")
            findNavController().navigate(accion)
        }
    }

    private fun observarEstado() {
        modelo.estadoUI.asLiveData().observe(viewLifecycleOwner) { estado ->
            binding.progressBar.isVisible = estado is EstadoCatalogoUI.Cargando
            binding.layoutEmpty.isVisible = estado is EstadoCatalogoUI.Vacio
            binding.rvProducts.isVisible = estado is EstadoCatalogoUI.Exito

            when (estado) {
                is EstadoCatalogoUI.Exito -> adaptador.submitList(estado.productos)
                is EstadoCatalogoUI.Error -> {
                    // Mostrar error
                }
                else -> {}
            }
        }
    }

    private fun mostrarConfirmacionEliminar(idProducto: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.product_delete_confirm)
            .setMessage("¿Estás seguro de que deseas eliminar este producto?")
            .setPositiveButton("Eliminar") { _, _ ->
                modelo.eliminarProducto(idProducto)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}