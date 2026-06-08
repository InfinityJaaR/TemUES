package com.market.temues.data.remote.user

import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("users")

    fun getById(id: String): Flow<User?> = callbackFlow {
        val registration = collection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)?.copy(id = snapshot.id)
                trySend(user)
            }
        awaitClose { registration.remove() }
    }

    suspend fun save(user: User) {
        collection.document(user.id).set(user).await()
    }

    suspend fun update(id: String, data: Map<String, Any>) {
        collection.document(id).update(data).await()
    }
}
