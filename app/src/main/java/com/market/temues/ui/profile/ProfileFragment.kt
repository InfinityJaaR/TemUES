package com.market.temues.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.market.temues.R
import com.market.temues.databinding.FragmentProfileBinding
import com.market.temues.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val modeloAuth: AuthViewModel by viewModels()
    private val modeloPerfil: EditProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarEscuchadores()
        observarModelo()
    }

    private fun configurarEscuchadores() {
        binding.btnMyProducts.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_sellerCatalog)
        }

        binding.btnFavorites.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_favorites)
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_editProfile)
        }

        binding.btnPurchaseHistory.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_historial)
        }

        binding.btnLogout.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun observarModelo() {
        modeloPerfil.usuarioActual.asLiveData().observe(viewLifecycleOwner) { usuario ->
            usuario?.let {
                binding.tvName.text = it.name.ifEmpty { "Usuario" }
                binding.tvEmail.text = it.email
                
                Glide.with(this)
                    .load(it.photoUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .circleCrop()
                    .into(binding.ivProfilePhoto)
                
                binding.btnAdminPanel.isVisible = it.isAdmin
            }
        }
    }

    private fun cerrarSesion() {
        try {
            val opcionesGoogle = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            GoogleSignIn.getClient(requireActivity(), opcionesGoogle).signOut()
        } catch (_: Exception) { }
        
        modeloAuth.signOut()
        
        val opcionesNav = androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        findNavController().navigate(R.id.loginFragment, null, opcionesNav)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}