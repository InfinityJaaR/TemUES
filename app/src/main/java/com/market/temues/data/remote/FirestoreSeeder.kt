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
    private val adminSellerEmail = "yami@gmail.com"

    suspend fun seed(): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("Debes iniciar sesión primero"))

        val doc = firestore.collection("_meta").document(isSeededKey).get().await()
        if (doc.exists()) {
            return Result.failure(Exception("Los datos de prueba ya fueron cargados"))
        }

        return try {
            seedCategories()
            val seller = seedAdminSeller(currentUser)
            seedProductos(seller.id, seller.name.ifBlank { "Yami" }, 70)
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
            isAdmin = user.email.equals(adminSellerEmail, ignoreCase = true),
            createdAt = System.currentTimeMillis()
        )
        firestore.collection("users").document(user.uid).set(profile).await()
    }

    private suspend fun seedAdminSeller(currentUser: FirebaseUser): User {
        if (currentUser.email.equals(adminSellerEmail, ignoreCase = true)) {
            val admin = User(
                id = currentUser.uid,
                email = adminSellerEmail,
                name = currentUser.displayName ?: "Yami",
                photoUrl = currentUser.photoUrl?.toString() ?: "",
                isAdmin = true,
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("users").document(admin.id).set(admin).await()
            return admin
        }

        val existingAdmin = firestore.collection("users")
            .whereEqualTo("email", adminSellerEmail)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toObject(User::class.java)

        if (existingAdmin != null) {
            val admin = existingAdmin.copy(email = adminSellerEmail, isAdmin = true)
            firestore.collection("users").document(admin.id).set(admin).await()
            return admin
        }

        val adminDoc = firestore.collection("users").document("admin_yami")
        val admin = User(
            id = adminDoc.id,
            email = adminSellerEmail,
            name = "Yami",
            isAdmin = true,
            createdAt = System.currentTimeMillis()
        )
        adminDoc.set(admin).await()
        return admin
    }

    suspend fun cargarProductosDemo(cantidad: Int = 70): Result<Unit> {
        val currentUser = firebaseAuth.currentUser
            ?: return Result.failure(Exception("Debes iniciar sesión primero"))

        return try {
            seedCategories()
            val seller = seedAdminSeller(currentUser)
            seedProductos(seller.id, seller.name.ifBlank { "Yami" }, cantidad)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun seedProductos(sellerId: String, sellerName: String, cantidad: Int) {
        val base = listOf(
            Product(
                name = "iPhone 15 Pro 256GB",
                description = "En excelente estado, con funda y cargador. Batería al 92%.",
                price = 750.00,
                categoryId = "electronica",
                categoryName = "Electrónicos",
                sellerId = sellerId,
                sellerName = sellerName,
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
                sellerName = sellerName,
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
                sellerName = sellerName,
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
                sellerName = sellerName,
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
                sellerName = sellerName,
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
                sellerName = sellerName,
                condition = "usado",
                location = "San Salvador",
                tags = listOf("tv", "samsung", "4k"),
                status = "activo"
            ),
            Product(
                name = "Toyota Corolla 2012",
                description = "Automático, aire acondicionado, papeles al día.",
                price = 6200.00,
                categoryId = "vehiculos",
                categoryName = "Vehículos",
                sellerId = sellerId,
                sellerName = sellerName,
                condition = "usado",
                location = "San Miguel",
                tags = listOf("carro", "toyota", "vehiculo"),
                status = "activo"
            ),
            Product(
                name = "Servicio de tutorías de programación",
                description = "Clases personalizadas de Kotlin, Java y bases de datos.",
                price = 12.00,
                categoryId = "servicios",
                categoryName = "Servicios",
                sellerId = sellerId,
                sellerName = sellerName,
                condition = "nuevo",
                location = "UES / En línea",
                tags = listOf("tutorias", "programacion", "servicio"),
                status = "activo"
            ),
            Product(
                name = "Combo de útiles universitarios",
                description = "Cuadernos, lapiceros, folders y calculadora básica.",
                price = 18.00,
                categoryId = "otros",
                categoryName = "Otros",
                sellerId = sellerId,
                sellerName = sellerName,
                condition = "nuevo",
                location = "San Salvador",
                tags = listOf("utiles", "universidad", "otros"),
                status = "activo"
            )
        )

        val nombresUnicos = listOf(
            "iPhone 15 Pro 256GB", "MacBook Air M2", "Silla Gamer Reclinable", "Bicicleta Montañera 29\"",
            "Zapatos Nike Air Max", "TV Samsung 55\" 4K", "Toyota Corolla 2012", "Tutorías de Programación",
            "Combo de útiles universitarios", "Audífonos Sony WH-1000XM4", "iPad Air 5ta Gen", "Monitor LG Ultrawide 29\"",
            "Teclado Mecánico Redragon", "Mouse Logitech MX Master", "Cámara Canon Rebel T7", "Nintendo Switch OLED",
            "PlayStation 5 Slim", "Apple Watch Series 8", "Samsung Galaxy S23", "Laptop Lenovo ThinkPad",
            "Escritorio de madera", "Sofá cama gris", "Mesa plegable", "Lámpara LED de escritorio",
            "Ventilador de torre", "Microondas Panasonic", "Licuadora Oster", "Cafetera Hamilton Beach",
            "Set de ollas Tramontina", "Colchón matrimonial", "Camisa formal azul", "Jeans Levi's 511",
            "Chaqueta impermeable", "Mochila universitaria", "Reloj Casio Vintage", "Gorra Adidas original",
            "Tenis Puma Runner", "Vestido casual", "Bolso de cuero", "Sudadera UES",
            "Balón de fútbol Adidas", "Raqueta de tenis Wilson", "Mancuernas 20 lb", "Patineta profesional",
            "Casco para bicicleta", "Guantes de boxeo", "Tienda de campaña", "Set de pesca",
            "Scooter eléctrico", "Bicicleta BMX", "Honda Civic 2009", "Yamaha FZ 2018",
            "Casco certificado DOT", "Rin deportivo 17\"", "Batería para carro", "Compresor portátil",
            "Servicio de diseño gráfico", "Clases de inglés", "Reparación de celulares", "Instalación de Windows",
            "Asesoría de tesis", "Fotografía para eventos", "Transporte universitario", "Impresiones y anillados",
            "Calculadora científica Casio", "Libro de Cálculo Stewart", "Kit de dibujo técnico", "Uniforme de laboratorio",
            "Memoria USB 128GB", "Disco externo 1TB"
        )

        val descripciones = listOf(
            "Producto en excelente estado, listo para entrega inmediata.",
            "Incluye accesorios principales y se entrega probado.",
            "Ideal para estudiantes universitarios de la UES.",
            "Precio negociable, entrega en San Salvador o alrededores.",
            "Disponible para coordinar por chat dentro de la app."
        )
        val ubicaciones = listOf("San Salvador", "Santa Tecla", "Antiguo Cuscatlán", "San Miguel", "UES / En línea")

        val productos = List(cantidad) { index ->
            val baseProducto = base[index % base.size]
            val nombre = nombresUnicos[index % nombresUnicos.size]
            val numero = index + 1
            val categoria = categoriaParaProducto(nombre)
            baseProducto.copy(
                name = nombre,
                description = "${descripciones[index % descripciones.size]} Código demo $numero.",
                price = baseProducto.price + (index + 1) * 2.75,
                categoryId = categoria.first,
                categoryName = categoria.second,
                images = emptyList(),
                location = ubicaciones[index % ubicaciones.size],
                condition = if (index % 3 == 0) "nuevo" else "usado",
                tags = listOf("demo", "producto$numero", nombre.lowercase(), categoria.first)
            )
        }

        productos.chunked(450).forEachIndexed { chunkIndex, chunk ->
            val batch = firestore.batch()
            chunk.forEachIndexed { indexInChunk, prod ->
                val numero = chunkIndex * 450 + indexInChunk + 1
                val docId = "demo_yami_${numero.toString().padStart(2, '0')}"
                val docRef = firestore.collection("products").document(docId)
                batch.set(docRef, prod.copy(id = docId))
            }
            batch.commit().await()
        }
    }

    private fun categoriaParaProducto(nombre: String): Pair<String, String> {
        val texto = nombre.lowercase()
        return when {
            listOf("iphone", "macbook", "audífonos", "ipad", "monitor", "teclado", "mouse", "cámara", "nintendo", "playstation", "apple watch", "galaxy", "laptop", "usb", "disco externo").any { texto.contains(it) } ->
                "electronica" to "Electrónicos"
            listOf("zapatos", "camisa", "jeans", "chaqueta", "mochila", "reloj", "gorra", "tenis", "vestido", "bolso", "sudadera", "uniforme").any { texto.contains(it) } ->
                "ropa" to "Ropa y Accesorios"
            listOf("silla", "escritorio", "sofá", "mesa", "lámpara", "ventilador", "microondas", "licuadora", "cafetera", "ollas", "colchón").any { texto.contains(it) } ->
                "hogar" to "Hogar y Muebles"
            listOf("bicicleta", "balón", "raqueta", "mancuernas", "patineta", "casco para bicicleta", "boxeo", "campaña", "pesca", "bmx").any { texto.contains(it) } ->
                "deportes" to "Deportes y Ocio"
            listOf("toyota", "scooter", "honda", "yamaha", "casco certificado", "rin", "batería para carro", "compresor").any { texto.contains(it) } ->
                "vehiculos" to "Vehículos"
            listOf("tutorías", "servicio", "clases", "reparación", "instalación", "asesoría", "fotografía", "transporte", "impresiones").any { texto.contains(it) } ->
                "servicios" to "Servicios"
            else -> "otros" to "Otros"
        }
    }
}
