package com.market.temues.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.data.local.dao.FavoriteDao
import com.market.temues.data.local.entity.FavoriteEntity
import com.market.temues.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val firestore: FirebaseFirestore
) {
    private fun userFavoritesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("favorites")

    fun getAll(userId: String): Flow<List<FavoriteEntity>> = favoriteDao.getAll(userId)
    fun isFavorite(userId: String, productId: String): Flow<Boolean> = favoriteDao.isFavorite(userId, productId)

    suspend fun addFavorite(userId: String, product: Product) {
        val fav = FavoriteEntity(
            userId = userId,
            productId = product.id,
            productName = product.name,
            productPrice = product.price,
            productImage = product.images.firstOrNull() ?: "",
            isSynced = false
        )
        favoriteDao.insert(fav)
        try {
            userFavoritesCollection(userId).document(product.id).set(fav).await()
            favoriteDao.markAsSynced(userId, product.id)
        } catch (_: Exception) { }
    }

    suspend fun removeFavorite(userId: String, productId: String) {
        favoriteDao.delete(userId, productId)
        try {
            userFavoritesCollection(userId).document(productId).delete().await()
        } catch (_: Exception) { }
    }

    suspend fun toggleFavorite(userId: String, product: Product) {
        if (favoriteDao.isFavoriteSync(userId, product.id)) {
            removeFavorite(userId, product.id)
        } else {
            addFavorite(userId, product)
        }
    }

    suspend fun syncPendingFavorites(userId: String) {
        val unsynced = favoriteDao.getUnsynced(userId)
        for (fav in unsynced) {
            try {
                userFavoritesCollection(userId).document(fav.productId).set(fav).await()
                favoriteDao.markAsSynced(userId, fav.productId)
            } catch (_: Exception) { }
        }
    }
}