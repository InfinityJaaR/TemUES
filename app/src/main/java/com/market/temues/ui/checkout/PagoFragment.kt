package com.market.temues.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.market.temues.R
import com.market.temues.databinding.PantallaCheckoutBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PagoFragment : Fragment() {

    private var _binding: PantallaCheckoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PagoViewModel by viewModels()
    private lateinit var adaptadorResumen: ResumenOrdenAdapter

    // Bloquea el botón atrás mientras se procesa el pedido
    private val bloqueadorAtras = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() { /* bloqueado intencionalmente durante el procesamiento */ }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PantallaCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, bloqueadorAtras)
        configurarRecyclerView()
        observarEstado()
        configurarMetodoPago()
        configurarBotonConfirmar()
    }

    private fun configurarRecyclerView() {
        adaptadorResumen = ResumenOrdenAdapter()
        binding.rvResumen.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adaptadorResumen
        }
    }

    private fun observarEstado() {
        viewModel.articulos.asLiveData().observe(viewLifecycleOwner) { articulos ->
            adaptadorResumen.submitList(articulos)
            val lugarEntrega = articulos.firstOrNull()?.lugarEntrega
                ?.takeIf { it.isNotBlank() }
                ?: getString(R.string.checkout_coordinar_chat)
            binding.txtLugarEntrega.text = lugarEntrega
        }

        viewModel.total.asLiveData().observe(viewLifecycleOwner) { total ->
            binding.txtTotalCheckout.text = "$%.2f".format(total)
        }

        viewModel.cargando.asLiveData().observe(viewLifecycleOwner) { cargando ->
            bloqueadorAtras.isEnabled = cargando
            binding.progressCheckout.visibility = if (cargando) View.VISIBLE else View.GONE
            binding.btnConfirmarPedido.isEnabled = !cargando
        }

        viewModel.resultadoOrden.asLiveData().observe(viewLifecycleOwner) { resultado ->
            resultado ?: return@observe
            when (resultado) {
                is PagoViewModel.ResultadoOrden.Exitoso -> mostrarDialogoExito(resultado.codigo)
                is PagoViewModel.ResultadoOrden.Error -> mostrarDialogoError(resultado.mensaje)
            }
        }
    }

    private fun configurarMetodoPago() {
        binding.rgMetodoPago.setOnCheckedChangeListener { _, checkedId ->
            viewModel.metodoPago.value = when (checkedId) {
                R.id.rb_tarjeta -> "tarjeta"
                else -> "efectivo"
            }
        }
    }

    private fun configurarBotonConfirmar() {
        binding.btnConfirmarPedido.setOnClickListener {
            viewModel.confirmarPedido()
        }
    }

    private fun mostrarDialogoExito(codigo: String) {
        viewModel.resetResultado()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.checkout_pedido_exitoso))
            .setMessage(getString(R.string.checkout_codigo_mensaje, codigo))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.checkout_ver_compras)) { _, _ ->
                // homeFragment siempre está en el stack (post-login), así que
                // esto limpia todo (cart, checkout, productDetail) sin importar
                // el camino tomado para llegar aquí
                val opciones = NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, false)
                    .build()
                findNavController().navigate(R.id.historialFragment, null, opciones)
            }
            .show()
    }

    private fun mostrarDialogoError(mensaje: String) {
        viewModel.resetResultado()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
