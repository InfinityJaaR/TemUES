package com.market.temues.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.model.Category
import com.market.temues.model.Product
import com.market.temues.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSeeder @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    private val isSeededKey = "temues_seeded"

    suspend fun seed(): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("Debes iniciar sesión primero"))

        val doc = firestore.collection("_meta").document(isSeededKey).get().await()
        if (doc.exists()) {
            return Result.failure(Exception("Los datos de prueba ya fueron cargados"))
        }

        return try {
            seedCategories()
            seedCurrentUser(currentUser)
            seedProductos(currentUser.uid)
            firestore.collection("_meta").document(isSeededKey)
                .set(mapOf("seeded" to true)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun seedCategories() {
        val categorias = listOf(
            Category("electronica", "Electrónicos", "", null, 1),
            Category("ropa", "Ropa y Accesorios", "", null, 2),
            Category("hogar", "Hogar y Muebles", "", null, 3),
            Category("deportes", "Deportes y Ocio", "", null, 4),
            Category("vehiculos", "Vehículos", "", null, 5),
            Category("servicios", "Servicios", "", null, 6),
            Category("otros", "Otros", "", null, 7)
        )
        for (cat in categorias) {
            firestore.collection("categories").document(cat.id).set(cat).await()
        }
    }

    private suspend fun seedCurrentUser(user: FirebaseUser) {
        val profile = User(
            id = user.uid,
            email = user.email ?: "",
            name = user.displayName ?: "Usuario TemUES",
            photoUrl = user.photoUrl?.toString() ?: "",
            createdAt = System.currentTimeMillis()
        )
        firestore.collection("users").document(user.uid).set(profile).await()
    }

    private suspend fun seedProductos(sellerId: String) {
        val productos = listOf(
            Product(
                name = "iPhone 15 Pro 256GB",
                description = "En excelente estado, con funda y cargador. Batería al 92%.",
                price = 750.00,
                categoryId = "electronica",
                categoryName = "Electrónicos",
                sellerId = sellerId,
                sellerName = "Carlos López",
                condition = "usado",
                location = "San Salvador",
                tags = listOf("iphone", "apple", "celular"),
                status = "activo"
            ),
            Product(
                name = "MacBook Air M2",
                description = "Nueva, sellada. 8GB RAM, 256GB SSD.",
                price = 950.00,
                categoryId = "electronica",
                categoryName = "Electrónicos",
                sellerId = sellerId,
                sellerName = "Carlos López",
                condition = "nuevo",
                location = "Santa Tecla",
                tags = listOf("macbook", "apple", "laptop"),
                status = "activo"
            ),
            Product(
                name = "Silla Gamer Reclinable",
                description = "Usada 3 meses, como nueva. Ajustable en altura y reposabrazos.",
                price = 120.00,
                categoryId = "hogar",
                categoryName = "Hogar y Muebles",
                sellerId = sellerId,
                sellerName = "Carlos López",
                condition = "usado",
                location = "San Salvador",
                tags = listOf("silla", "gamer", "escritorio"),
                status = "activo"
            ),
            Product(
                name = "Bicicleta Montañera 29\"",
                description = "Suspensión delantera, cambios Shimano 21 velocidades.",
                price = 250.00,
                categoryId = "deportes",
                categoryName = "Deportes y Ocio",
                sellerId = sellerId,
                sellerName = "Carlos López",
                condition = "usado",
                location = "Antiguo Cuscatlán",
                tags = listOf("bicicleta", "montaña", "deporte"),
                status = "activo"
            ),
            Product(
                name = "Zapatos Nike Air Max",
                description = "Talla 42, nuevos con caja. Originales.",
                price = 85.00,
                categoryId = "ropa",
                categoryName = "Ropa y Accesorios",
                sellerId = sellerId,
                sellerName = "Carlos López",
                condition = "nuevo",
                location = "San Salvador",
                tags = listOf("nike", "zapatos", "deporte"),
                status = "activo"
            ),
            Product(
                name = "TV Samsung 55\" 4K",
                description = "Smart TV, modelo 2024. Control remoto incluido.",
                price = 500.00,
                categoryId = "electronica",
                categoryName = "Electrónicos",
                sellerId = sellerId,
                sellerName = "Carlos López",
                condition = "usado",
                location = "San Salvador",
                tags = listOf("tv", "samsung", "4k"),
                status = "activo"
            )
        )

        for (prod in productos) {
            val docRef = firestore.collection("products").document()
            docRef.set(prod.copy(id = docRef.id)).await()
        }
    }
}
