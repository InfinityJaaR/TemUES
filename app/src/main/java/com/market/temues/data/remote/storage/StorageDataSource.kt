package com.market.temues.data.remote.storage

import android.net.Uri
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
    private val storage: FirebaseStorage
) {
    private val productsRef = storage.reference.child("products")
    private val avatarsRef = storage.reference.child("avatars")

    fun uploadProductImage(imageUri: Uri): Flow<Result<String>> = callbackFlow {
        val fileName = "product_${UUID.randomUUID()}.jpg"
        productsRef.child(fileName).putFile(imageUri)
            .addOnSuccessListener {
                productsRef.child(fileName).downloadUrl
                    .addOnSuccessListener { uri -> trySend(Result.success(uri.toString())) }
                    .addOnFailureListener { e -> trySend(Result.failure(e)) }
            }
            .addOnFailureListener { e -> trySend(Result.failure(e)) }
        awaitClose { }
    }

    fun uploadAvatar(userId: String, imageUri: Uri): Flow<Result<String>> = callbackFlow {
        avatarsRef.child("${userId}.jpg").putFile(imageUri)
            .addOnSuccessListener {
                avatarsRef.child("${userId}.jpg").downloadUrl
                    .addOnSuccessListener { uri -> trySend(Result.success(uri.toString())) }
                    .addOnFailureListener { e -> trySend(Result.failure(e)) }
            }
            .addOnFailureListener { e -> trySend(Result.failure(e)) }
        awaitClose { }
    }

    suspend fun deleteImage(url: String) {
        try {
            storage.getReferenceFromUrl(url).delete().await()
        } catch (_: Exception) { }
    }
}
