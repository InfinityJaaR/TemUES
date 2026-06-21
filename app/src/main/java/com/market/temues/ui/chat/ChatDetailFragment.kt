package com.market.temues.ui.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.market.temues.databinding.PantallaDetalleChatBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatDetailFragment : Fragment() {

    private var _binding: PantallaDetalleChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var adaptadorMensajes: MensajesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PantallaDetalleChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarRecyclerView()
        configurarInput()
        observarEstado()
        observarNombreUsuario()
    }

    private fun configurarRecyclerView() {
        adaptadorMensajes = MensajesAdapter(uidActual = viewModel.uidActual)
        binding.recyclerMensajes.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = adaptadorMensajes
        }
    }

    private fun configurarInput() {
        binding.inputMensaje.doAfterTextChanged { texto ->
            binding.btnEnviarMensaje.isEnabled = !texto.isNullOrBlank()
        }
        binding.btnEnviarMensaje.isEnabled = false

        binding.btnEnviarMensaje.setOnClickListener {
            val texto = binding.inputMensaje.text?.toString().orEmpty()
            if (texto.isNotBlank()) {
                viewModel.enviarMensaje(texto)
                binding.inputMensaje.setText("")
            }
        }

        binding.btnLlamar.setOnClickListener {
            val telefono = viewModel.telefonoOtroUsuario.value
            if (telefono.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$telefono")
                }
                startActivity(intent)
            }
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estadoUi.collect { estado ->
                    when (estado) {
                        EstadoDetalleChat.Cargando -> mostrarCargando()
                        is EstadoDetalleChat.Exito -> {
                            ocultarCargando()
                            adaptadorMensajes.submitList(estado.mensajes) {
                                if (estado.mensajes.isNotEmpty()) {
                                    binding.recyclerMensajes.scrollToPosition(estado.mensajes.size - 1)
                                }
                            }
                        }
                        is EstadoDetalleChat.Error -> mostrarError(estado.mensaje)
                    }
                }
            }
        }
    }

    private fun observarNombreUsuario() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.nombreOtroUsuario.collect { nombre ->
                    binding.txtNombreOtroUsuario.text = nombre.ifBlank { "Chat" }
                    binding.txtInicialAvatar.text = nombre.firstOrNull()?.uppercase() ?: "?"
                }
            }
        }
    }

    private fun mostrarCargando() {
        binding.animacionCargaDetalle.isVisible = true
        binding.recyclerMensajes.isVisible = false
    }

    private fun ocultarCargando() {
        binding.animacionCargaDetalle.isVisible = false
        binding.recyclerMensajes.isVisible = true
    }

    private fun mostrarError(mensaje: String) {
        binding.animacionCargaDetalle.isVisible = false
        binding.recyclerMensajes.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
