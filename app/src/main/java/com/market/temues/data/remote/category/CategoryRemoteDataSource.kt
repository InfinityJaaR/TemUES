package com.market.temues.data.remote.category

import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.model.Category
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("categories")

    fun getAll(): Flow<List<Category>> = callbackFlow {
        val registration = collection
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull {
                    it.toObject(Category::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { registration.remove() }
    }

    fun getById(id: String): Flow<Category?> = callbackFlow {
        val registration = collection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val category = snapshot?.toObject(Category::class.java)?.copy(id = snapshot.id)
                trySend(category)
            }
        awaitClose { registration.remove() }
    }

    suspend fun create(category: Category): String {
        val docRef = collection.document()
        docRef.set(category.copy(id = docRef.id)).await()
        return docRef.id
    }

    suspend fun update(category: Category) {
        collection.document(category.id).set(category).await()
    }

    suspend fun getMaxOrder(): Int {
        val snapshot = collection.orderBy("order", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.getLong("order")?.toInt() ?: 0
    }
}
