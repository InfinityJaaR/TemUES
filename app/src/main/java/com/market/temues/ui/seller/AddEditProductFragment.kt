package com.market.temues.ui.seller

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.market.temues.R
import com.market.temues.databinding.FragmentAddEditProductBinding
import com.market.temues.ui.camera.CameraFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddEditProductFragment : Fragment() {

    private var _binding: FragmentAddEditProductBinding? = null
    private val binding get() = _binding!!

    private val modelo: AddEditProductViewModel by viewModels()
    private val argumentos: AddEditProductFragmentArgs by navArgs()

    private val permisoCamaraLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) navegarACamara()
    }

    private val galeriaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { modelo.subirImagen(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val adaptadorImagenes = ProductImageAdapter { index ->
        modelo.eliminarImagen(index)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListener(CameraFragment.RESULTADO_FOTO) { _, bundle ->
            val uriString = bundle.getString(CameraFragment.CLAVE_URI) ?: return@setFragmentResultListener
            modelo.subirImagen(Uri.parse(uriString))
        }
        configurarSelectorCategorias()
        configurarListaImagenes()
        configurarEscuchadores()
        observarModelo()
    }

    private fun configurarSelectorCategorias() {
        modelo.categorias.asLiveData().observe(viewLifecycleOwner) { listaCategorias ->
            val nombres = listaCategorias.map { it.name }
            val adaptador = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
            binding.actvCategory.setAdapter(adaptador)
            binding.actvCategory.setOnItemClickListener { _, _, posicion, _ ->
                modelo.idCategoria.value = listaCategorias[posicion].id
            }
        }
    }

    private fun configurarListaImagenes() {
        binding.rvImages.adapter = adaptadorImagenes
    }

    private fun mostrarSelectorOrigen() {
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar foto")
            .setItems(arrayOf("Tomar foto", "Subir desde galería")) { _, which ->
                when (which) {
                    0 -> abrirCamara()
                    1 -> galeriaLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun abrirCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        navegarACamara()
    }

    private fun navegarACamara() {
        findNavController().navigate(R.id.action_addEditProduct_to_camera)
    }

    private fun configurarEscuchadores() {
        binding.btnAddImage.setOnClickListener { mostrarSelectorOrigen() }

        binding.switchHasStock.setOnCheckedChangeListener { _, isChecked ->
            binding.tilStock.isVisible = isChecked
        }

        binding.btnSave.setOnClickListener {
            modelo.nombre.value = binding.etName.text.toString()
            modelo.descripcion.value = binding.etDescription.text.toString()
            modelo.precio.value = binding.etPrice.text.toString()
            modelo.ubicacion.value = binding.etLocation.text.toString()
            modelo.condicion.value = if (binding.rbNew.isChecked) "nuevo" else "usado"
            modelo.tieneStock.value = binding.switchHasStock.isChecked
            modelo.cantidadStock.value = binding.etStock.text.toString()
            modelo.guardar()
        }
    }

    private fun observarModelo() {
        modelo.estadoGuardado.asLiveData().observe(viewLifecycleOwner) { estado ->
            binding.btnSave.isEnabled = estado !is EstadoGuardado.Guardando
            when (estado) {
                is EstadoGuardado.Exito -> {
                    Toast.makeText(requireContext(), "Producto guardado", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is EstadoGuardado.Error -> {
                    Toast.makeText(requireContext(), estado.mensaje, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        modelo.imagenes.asLiveData().observe(viewLifecycleOwner) { lista ->
            adaptadorImagenes.submitList(lista)
        }

        // Prellenar si es edición
        if (!argumentos.productId.isNullOrEmpty()) {
            modelo.nombre.asLiveData().observe(viewLifecycleOwner) { if (binding.etName.text.isNullOrEmpty()) binding.etName.setText(it) }
            modelo.descripcion.asLiveData().observe(viewLifecycleOwner) { if (binding.etDescription.text.isNullOrEmpty()) binding.etDescription.setText(it) }
            modelo.precio.asLiveData().observe(viewLifecycleOwner) { if (binding.etPrice.text.isNullOrEmpty()) binding.etPrice.setText(it) }
            modelo.ubicacion.asLiveData().observe(viewLifecycleOwner) { if (binding.etLocation.text.isNullOrEmpty()) binding.etLocation.setText(it) }
            modelo.condicion.asLiveData().observe(viewLifecycleOwner) {
                if (it == "nuevo") binding.rbNew.isChecked = true else binding.rbUsed.isChecked = true
            }
            modelo.tieneStock.asLiveData().observe(viewLifecycleOwner) {
                binding.switchHasStock.isChecked = it
                binding.tilStock.isVisible = it
            }
            modelo.cantidadStock.asLiveData().observe(viewLifecycleOwner) {
                if (binding.etStock.text.isNullOrEmpty()) binding.etStock.setText(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}