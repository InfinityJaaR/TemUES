package com.market.temues.data.remote.storage

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage,
    private val firebaseAuth: FirebaseAuth
) {
    private val productsRef = storage.reference.child("products")
    private val avatarsRef = storage.reference.child("avatars")

    fun uploadProductImage(imageUri: Uri): Flow<Result<String>> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            trySend(Result.failure(Exception("Debes iniciar sesión para subir imágenes")))
            close()
            return@callbackFlow
        }

        val fileName = "product_${UUID.randomUUID()}.jpg"
        val imageRef = productsRef.child(userId).child(fileName)
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                trySend(Result.success(imageRef.path))
            }
            .addOnFailureListener { e -> trySend(Result.failure(e)) }
        awaitClose { }
    }

    fun uploadAvatar(userId: String, imageUri: Uri): Flow<Result<String>> = callbackFlow {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null || currentUserId != userId) {
            trySend(Result.failure(Exception("Solo puedes subir tu propio avatar")))
            close()
            return@callbackFlow
        }

        val imageRef = avatarsRef.child(userId).child("avatar.jpg")
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                trySend(Result.success(imageRef.path))
            }
            .addOnFailureListener { e -> trySend(Result.failure(e)) }
        awaitClose { }
    }

    fun getImageUrl(path: String): Flow<Result<String>> = callbackFlow {
        val reference = if (path.startsWith("gs://") || path.startsWith("https://")) {
            storage.getReferenceFromUrl(path)
        } else {
            storage.reference.child(path)
        }

        reference.downloadUrl
            .addOnSuccessListener { uri -> trySend(Result.success(uri.toString())) }
            .addOnFailureListener { e -> trySend(Result.failure(e)) }
        awaitClose { }
    }

    suspend fun deleteImage(path: String) {
        try {
            val reference = if (path.startsWith("gs://") || path.startsWith("https://")) {
                storage.getReferenceFromUrl(path)
            } else {
                storage.reference.child(path)
            }
            reference.delete().await()
        } catch (_: Exception) { }
    }
}
