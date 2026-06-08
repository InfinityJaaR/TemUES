package com.market.temues.data.remote.product

import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("products")

    fun getAll(): Flow<List<Product>> = callbackFlow {
        val registration = collection
            .whereEqualTo("status", "activo")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val products = snapshot?.documents?.mapNotNull {
                    it.toObject(Product::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(products)
            }
        awaitClose { registration.remove() }
    }

    fun getById(id: String): Flow<Product?> = callbackFlow {
        val registration = collection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val product = snapshot?.toObject(Product::class.java)?.copy(id = snapshot.id)
                trySend(product)
            }
        awaitClose { registration.remove() }
    }

    fun getByCategory(categoryId: String): Flow<List<Product>> = callbackFlow {
        val registration = collection
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("status", "activo")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val products = snapshot?.documents?.mapNotNull {
                    it.toObject(Product::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(products)
            }
        awaitClose { registration.remove() }
    }

    fun search(query: String): Flow<List<Product>> = callbackFlow {
        val registration = collection
            .whereEqualTo("status", "activo")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val products = snapshot?.documents?.mapNotNull {
                    it.toObject(Product::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(products)
            }
        awaitClose { registration.remove() }
    }

    fun getBySeller(sellerId: String): Flow<List<Product>> = callbackFlow {
        val registration = collection
            .whereEqualTo("sellerId", sellerId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val products = snapshot?.documents?.mapNotNull {
                    it.toObject(Product::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(products)
            }
        awaitClose { registration.remove() }
    }

    suspend fun create(product: Product): String {
        val docRef = collection.document()
        product.copy(id = docRef.id).let {
            docRef.set(it).await()
        }
        return docRef.id
    }

    suspend fun update(product: Product) {
        collection.document(product.id).set(product.copy(updatedAt = System.currentTimeMillis())).await()
    }

    suspend fun delete(id: String) {
        collection.document(id).delete().await()
    }
}
