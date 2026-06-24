package com.market.temues.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.market.temues.R
import com.market.temues.databinding.PantallaListaChatsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatListFragment : Fragment() {

    private var _binding: PantallaListaChatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatListViewModel by viewModels()
    private lateinit var adaptadorChats: ChatListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PantallaListaChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarRecyclerView()
        observarEstado()
        observarNombres()
        observarUrlsProductos()
        binding.actualizarChats.setOnRefreshListener { viewModel.cargarChats() }
    }

    private fun configurarRecyclerView() {
        adaptadorChats = ChatListAdapter(
            uidActual = viewModel.uidActual,
            alHacerClickEnChat = { chat ->
                val args = Bundle().apply { putString("chatId", chat.id) }
                findNavController().navigate(R.id.action_chat_to_chatDetail, args)
            }
        )
        binding.recyclerChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adaptadorChats
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estadoUi.collect { estado ->
                    binding.actualizarChats.isRefreshing = false
                    when (estado) {
                        EstadoListaChat.Cargando -> mostrarCargando()
                        is EstadoListaChat.Exito -> {
                            ocultarEstados()
                            adaptadorChats.submitList(estado.chats)
                        }
                        is EstadoListaChat.Vacio -> mostrarVacio(estado.mensaje)
                        is EstadoListaChat.Error -> mostrarError(estado.mensaje)
                    }
                }
            }
        }
    }

    private fun observarNombres() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.nombresUsuarios.collect { nombres ->
                    adaptadorChats.actualizarNombres(nombres)
                }
            }
        }
    }

    private fun observarUrlsProductos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.urlsProductos.collect { urls ->
                    adaptadorChats.actualizarUrlsProductos(urls)
                }
            }
        }
    }

    private fun mostrarCargando() {
        binding.animacionCargaChats.isVisible = true
        binding.txtEstadoChats.isVisible = false
        binding.recyclerChats.isVisible = false
    }

    private fun ocultarEstados() {
        binding.animacionCargaChats.isVisible = false
        binding.txtEstadoChats.isVisible = false
        binding.recyclerChats.isVisible = true
    }

    private fun mostrarVacio(mensaje: String) {
        binding.animacionCargaChats.isVisible = false
        binding.txtEstadoChats.isVisible = true
        binding.txtEstadoChats.text = mensaje
        binding.recyclerChats.isVisible = false
    }

    private fun mostrarError(mensaje: String) {
        binding.animacionCargaChats.isVisible = false
        binding.txtEstadoChats.isVisible = true
        binding.txtEstadoChats.text = mensaje
        binding.recyclerChats.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
