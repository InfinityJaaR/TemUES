package com.market.temues.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.market.temues.databinding.FragmentFavoritesBinding
import com.market.temues.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val modelo: FavoritesViewModel by viewModels()
    private lateinit var adaptador: FavoriteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarLista()
        observarModelo()
        
        // Intentar sincronizar al entrar
        modelo.sincronizarFavoritos()
    }

    private fun configurarLista() {
        adaptador = FavoriteAdapter(
            onClick = { favorito ->
                val accion = FavoritesFragmentDirections.actionFavoritesToProductDetail(favorito.productId)
                findNavController().navigate(accion)
            },
            onRemove = { favorito ->
                modelo.eliminarFavorito(favorito.productId)
            }
        )
        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FavoritesFragment.adaptador
        }
    }

    private fun observarModelo() {
        modelo.favoritos.asLiveData().observe(viewLifecycleOwner) { listaFavoritos ->
            adaptador.submitList(listaFavoritos)
            binding.layoutEmpty.isVisible = listaFavoritos.isEmpty()
            binding.rvFavorites.isVisible = listaFavoritos.isNotEmpty()
            
            // Usamos NetworkUtils para mostrar el aviso si no hay internet REAL
            val estaOffline = !NetworkUtils.isOnline(requireContext())
            val hayPendientes = listaFavoritos.any { !it.isSynced }
            
            binding.tvOfflineMode.isVisible = estaOffline || hayPendientes
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}