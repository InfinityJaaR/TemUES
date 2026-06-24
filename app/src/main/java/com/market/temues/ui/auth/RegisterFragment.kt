package com.market.temues.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.market.temues.R
import com.market.temues.databinding.PantallaRegistroBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: PantallaRegistroBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = PantallaRegistroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeAuthState()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateFields(name, email, password, confirmPassword)) {
                authViewModel.registerWithEmail(name, email, password)
            }
        }

        binding.tvLoginLink.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        binding.etName.addTextChangedListener { authViewModel.resetState() }
        binding.etEmail.addTextChangedListener { authViewModel.resetState() }
        binding.etPassword.addTextChangedListener { authViewModel.resetState() }
        binding.etConfirmPassword.addTextChangedListener { authViewModel.resetState() }
    }

    private fun validateFields(
        name: String, email: String, password: String, confirmPassword: String
    ): Boolean {
        var valid = true

        if (name.isBlank()) {
            binding.tilName.error = "Ingresa tu nombre"
            valid = false
        } else {
            binding.tilName.error = null
        }

        if (email.isBlank()) {
            binding.tilEmail.error = "Ingresa tu correo"
            valid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isBlank()) {
            binding.tilPassword.error = "Ingresa una contraseña"
            valid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Mínimo 6 caracteres"
            valid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword.isBlank()) {
            binding.tilConfirmPassword.error = "Confirma tu contraseña"
            valid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Las contraseñas no coinciden"
            valid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return valid
    }

    private fun observeAuthState() {
        authViewModel.uiState.asLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled = false
                }

                is AuthUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val destination = if (state.user.isAdmin) {
                        R.id.action_register_to_adminDashboard
                    } else {
                        R.id.action_register_to_home
                    }
                    findNavController().navigate(destination)
                }

                is AuthUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }

                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
