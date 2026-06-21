package com.market.temues.ml

import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.data.local.SearchHistoryLocalStore
import com.market.temues.model.Product
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationEngine @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val searchHistoryLocalStore: SearchHistoryLocalStore
) {
    suspend fun rankProducts(productos: List<Product>, uid: String?): List<Product> {
        if (productos.isEmpty()) return productos

        val busquedasFirestore = if (uid.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                firestore.collection("users")
                    .document(uid)
                    .collection("searchHistory")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.getString("query") }
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
            }.getOrDefault(emptyList())
        }

        val busquedas = (searchHistoryLocalStore.obtenerBusquedas() + busquedasFirestore).distinct()

        if (busquedas.isEmpty()) return productos

        return productos.sortedWith(
            compareByDescending<Product> { producto -> calcularPuntaje(producto, busquedas) }
                .thenByDescending { it.createdAt }
        )
    }

    private fun calcularPuntaje(producto: Product, busquedas: List<String>): Int {
        val textoProducto = listOf(
            producto.name,
            producto.description,
            producto.categoryId,
            producto.categoryName,
            producto.condition,
            producto.location,
            producto.tags.joinToString(" ")
        ).joinToString(" ").lowercase()

        var puntaje = 0
        busquedas.forEach { busqueda ->
            puntaje += when {
                producto.name.lowercase().contains(busqueda) -> 5
                producto.categoryName.lowercase().contains(busqueda) -> 4
                producto.tags.any { it.lowercase().contains(busqueda) } -> 3
                textoProducto.contains(busqueda) -> 1
                else -> 0
            }
        }
        return puntaje
    }
}
