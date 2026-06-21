package com.market.temues.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.market.temues.data.local.entity.FavoriteEntity
import com.market.temues.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repositorioFavoritos: FavoritesRepository,
    private val autenticacion: FirebaseAuth
) : ViewModel() {

    private val idUsuario = autenticacion.currentUser?.uid ?: ""

    val favoritos: StateFlow<List<FavoriteEntity>> = repositorioFavoritos.getAll(idUsuario)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun eliminarFavorito(idProducto: String) {
        viewModelScope.launch {
            repositorioFavoritos.removeFavorite(idUsuario, idProducto)
        }
    }

    fun sincronizarFavoritos() {
        viewModelScope.launch {
            if (idUsuario.isNotEmpty()) {
                repositorioFavoritos.syncPendingFavorites(idUsuario)
            }
        }
    }
}