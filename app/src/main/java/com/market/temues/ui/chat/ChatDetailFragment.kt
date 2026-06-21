package com.market.temues.ui.chat

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.market.temues.R
import com.market.temues.databinding.PantallaDetalleChatBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatDetailFragment : Fragment() {

    private var _binding: PantallaDetalleChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var adaptadorMensajes: MensajesAdapter

    private val lanzadorPermisoAudio = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        val mensaje = if (concedido)
            getString(R.string.chat_audio_muy_corto)
        else
            getString(R.string.chat_permiso_audio)
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

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
        configurarBotonGrabacion()
        observarEstado()
        observarNombreUsuario()
        observarEstadoGrabacion()
        observarReproduccion()
        observarEventos()
    }

    private fun configurarRecyclerView() {
        adaptadorMensajes = MensajesAdapter(
            uidActual = viewModel.uidActual,
            alReproducir = { mensaje -> viewModel.toggleReproduccion(mensaje) }
        )
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
            } else {
                Toast.makeText(requireContext(), getString(R.string.chat_sin_telefono), Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Suppress("ClickableViewAccessibility")
    private fun configurarBotonGrabacion() {
        binding.btnGrabarAudio.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.iniciarGrabacion()
                    } else {
                        lanzadorPermisoAudio.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (viewModel.estaGrabando.value) {
                        viewModel.detenerYEnviarAudio()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    viewModel.cancelarGrabacion()
                    true
                }
                else -> false
            }
        }
    }

    private fun observarEstadoGrabacion() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.estaGrabando.collect { grabando ->
                        binding.layoutInputMensaje.isVisible = !grabando
                        binding.txtIndicadorGrabacion.isVisible = grabando
                        binding.btnEnviarMensaje.isEnabled = !grabando
                    }
                }
                launch {
                    viewModel.segundosGrabacion.collect { segundos ->
                        if (viewModel.estaGrabando.value) {
                            val min = segundos / 60
                            val seg = segundos % 60
                            binding.txtIndicadorGrabacion.text =
                                "● %d:%02d".format(min, seg)
                        }
                    }
                }
            }
        }
    }

    private fun observarReproduccion() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.idMensajeReproduciendo.collect { id ->
                    adaptadorMensajes.actualizarReproduccion(id)
                }
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

    private fun observarEventos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventoUi.collect { mensaje ->
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
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
