package com.market.temues.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.market.temues.data.repository.AuthRepository
import com.market.temues.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.loginWithEmail(email, password).collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { AuthUiState.Success(it) },
                    onFailure = { AuthUiState.Error(it.message ?: "Error al iniciar sesión") }
                )
            }
        }
    }

    fun registerWithEmail(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.registerWithEmail(name, email, password).collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { AuthUiState.Success(it) },
                    onFailure = { AuthUiState.Error(it.message ?: "Error al registrarse") }
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle(idToken).collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { AuthUiState.Success(it) },
                    onFailure = { AuthUiState.Error(it.message ?: "Error con Google") }
                )
            }
        }
    }

    fun signInWithFacebook(accessToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithFacebook(accessToken).collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { AuthUiState.Success(it) },
                    onFailure = { AuthUiState.Error(it.message ?: "Error con Facebook") }
                )
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.sendPasswordReset(email).collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { AuthUiState.PasswordResetSent },
                    onFailure = { AuthUiState.Error(it.message ?: "Error al enviar correo") }
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState.Idle
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data object PasswordResetSent : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
