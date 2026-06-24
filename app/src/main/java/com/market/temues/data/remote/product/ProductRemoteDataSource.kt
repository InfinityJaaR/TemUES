package com.market.temues.data.remote.product

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val products = snapshot?.documents.orEmpty()
                    .map { it.toProduct() }
                    .filter { it.status.isBlank() || it.status.equals("activo", ignoreCase = true) }
                    .sortedByDescending { it.createdAt }
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
                trySend(snapshot?.toProduct())
            }
        awaitClose { registration.remove() }
    }

    fun getByCategory(categoryId: String): Flow<List<Product>> = callbackFlow {
        val registration = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val products = snapshot?.documents.orEmpty()
                    .map { it.toProduct() }
                    .filter { it.status.isBlank() || it.status.equals("activo", ignoreCase = true) }
                    .filter { it.categoryId.equals(categoryId, ignoreCase = true) }
                    .sortedByDescending { it.createdAt }
                trySend(products)
            }
        awaitClose { registration.remove() }
    }

    fun search(query: String): Flow<List<Product>> = callbackFlow {
        val normalizedQuery = query.trim().lowercase()
        val registration = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val products = snapshot?.documents.orEmpty()
                    .map { it.toProduct() }
                    .filter { it.status.isBlank() || it.status.equals("activo", ignoreCase = true) }
                    .filter { product ->
                        normalizedQuery.isBlank() || listOf(
                            product.name,
                            product.description,
                            product.categoryId,
                            product.categoryName,
                            product.sellerName,
                            product.condition,
                            product.location,
                            product.tags.joinToString(" ")
                        ).joinToString(" ").lowercase().contains(normalizedQuery)
                    }
                    .sortedByDescending { it.createdAt }
                trySend(products)
            }
        awaitClose { registration.remove() }
    }

    fun getBySeller(sellerId: String): Flow<List<Product>> = callbackFlow {
        val registration = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val products = snapshot?.documents.orEmpty()
                    .map { it.toProduct() }
                    .filter { it.sellerId == sellerId }
                    .sortedByDescending { it.createdAt }
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

    private fun DocumentSnapshot.toProduct(): Product = Product(
        id = getString("id").orEmpty().ifBlank { id },
        name = getString("name").orEmpty(),
        description = getString("description").orEmpty(),
        price = getDouble("price") ?: getLong("price")?.toDouble() ?: 0.0,
        categoryId = getString("categoryId").orEmpty(),
        categoryName = getString("categoryName").orEmpty(),
        sellerId = getString("sellerId").orEmpty(),
        sellerName = getString("sellerName").orEmpty(),
        images = getStringArray("images"),
        condition = getString("condition") ?: "usado",
        location = getString("location").orEmpty(),
        tags = getStringArray("tags"),
        status = getString("status") ?: "activo",
        hasStock = getBoolean("hasStock") ?: false,
        stock = getLong("stock")?.toInt() ?: 0,
        createdAt = getMillis("createdAt"),
        updatedAt = getMillis("updatedAt")
    )

    private fun DocumentSnapshot.getMillis(field: String): Long = when (val value = get(field)) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> 0L
    }

    private fun DocumentSnapshot.getStringArray(field: String): List<String> = when (val value = get(field)) {
        is List<*> -> value.mapNotNull { it?.toString() }
        is String -> listOf(value)
        else -> emptyList()
    }
}
