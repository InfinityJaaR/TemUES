package com.market.temues.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.market.temues.databinding.FragmentEditProfileBinding
import com.market.temues.ui.seller.EstadoGuardado
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val modelo: EditProfileViewModel by viewModels()

    // Cambiado a PickVisualMedia para mejor soporte de Google Photos y Colecciones
    private val selectorImagen = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { 
            // Otorgar permisos de persistencia para la URI si es necesario
            try {
                val flag = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(it, flag)
            } catch (_: Exception) {}
            
            modelo.actualizarFoto(it) 
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEscuchadores()
        observarModelo()
    }

    private fun configurarEscuchadores() {
        binding.fabChangePhoto.setOnClickListener {
            selectorImagen.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSaveProfile.setOnClickListener {
            val nombre = binding.etName.text.toString()
            val telefono = binding.etPhone.text.toString()
            val biografia = binding.etBio.text.toString()
            modelo.actualizarPerfil(nombre, telefono, biografia)
        }
    }

    private fun observeViewModel() {
        // Observamos el usuario para llenar los campos
        modelo.usuarioActual.asLiveData().observe(viewLifecycleOwner) { usuario ->
            usuario?.let {
                if (binding.etName.text.isNullOrEmpty()) binding.etName.setText(it.name)
                if (binding.etPhone.text.isNullOrEmpty()) binding.etPhone.setText(it.phone)
                if (binding.etBio.text.isNullOrEmpty()) binding.etBio.setText(it.bio)
                
                Glide.with(this)
                    .load(it.photoUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivProfilePhoto)

                val esFotoGoogle = it.photoUrl.contains("googleusercontent.com")
                binding.fabChangePhoto.isVisible = !esFotoGoogle
                binding.tvGooglePhotoHint.isVisible = esFotoGoogle
            }
        }

        // Observamos el estado de la FOTO (ahora independiente)
        modelo.estadoFoto.asLiveData().observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is EstadoGuardado.Guardando -> {
                    binding.fabChangePhoto.isEnabled = false
                    Toast.makeText(requireContext(), "Subiendo foto...", Toast.LENGTH_SHORT).show()
                }
                is EstadoGuardado.Exito -> {
                    binding.fabChangePhoto.isEnabled = true
                    Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show()
                    modelo.reiniciarEstadoFoto()
                }
                is EstadoGuardado.Error -> {
                    binding.fabChangePhoto.isEnabled = true
                    Toast.makeText(requireContext(), estado.mensaje, Toast.LENGTH_LONG).show()
                    modelo.reiniciarEstadoFoto()
                }
                else -> {}
            }
        }

        // Observamos el estado del PERFIL (guardar nombre, bio, etc.)
        modelo.estadoGuardado.asLiveData().observe(viewLifecycleOwner) { estado ->
            binding.btnSaveProfile.isEnabled = estado !is EstadoGuardado.Guardando
            when (estado) {
                is EstadoGuardado.Exito -> {
                    Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is EstadoGuardado.Error -> {
                    Toast.makeText(requireContext(), estado.mensaje, Toast.LENGTH_LONG).show()
                    modelo.reiniciarEstadoGuardado()
                }
                else -> {}
            }
        }
    }

    private fun observarModelo() {
        // Llamamos a la función que centraliza los observadores
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}