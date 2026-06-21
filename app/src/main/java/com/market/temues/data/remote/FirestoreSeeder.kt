package com.market.temues.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSeeder @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun seed(): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("Debes iniciar sesión primero"))

        return try {
            val admin = User(
                id = currentUser.uid,
                email = currentUser.email ?: "",
                name = currentUser.displayName ?: "Admin",
                photoUrl = currentUser.photoUrl?.toString() ?: "",
                isAdmin = true
            )
            firestore.collection("users").document(currentUser.uid).set(admin).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
