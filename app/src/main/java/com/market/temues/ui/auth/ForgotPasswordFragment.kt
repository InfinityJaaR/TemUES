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
import com.market.temues.databinding.FragmentForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeAuthState()
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotBlank()) {
                authViewModel.sendPasswordReset(email)
            } else {
                binding.tilEmail.error = "Ingresa tu correo"
            }
        }

        binding.btnBackToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.etEmail.addTextChangedListener {
            binding.tilEmail.error = null
            authViewModel.resetState()
        }
    }

    private fun observeAuthState() {
        authViewModel.uiState.asLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSend.isEnabled = false
                }

                is AuthUiState.PasswordResetSent -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                    Snackbar.make(
                        binding.root,
                        "Correo de recuperación enviado. Revisa tu bandeja de entrada.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                is AuthUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }

                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
