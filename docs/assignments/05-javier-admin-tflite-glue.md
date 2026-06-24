# Javier — Admin Dashboard, TensorFlow Lite, RecommendationEngine, Categorías e Integración de Navegación

## Objetivo

Implementar las funcionalidades de administración (dashboard con estadísticas, análisis de tendencias con TensorFlow Lite, gestión de categorías), crear el **RecommendationEngine** que Yami usará para rankear productos en Home, proveer utilidades compartidas para el equipo, y consolidar la navegación global de la app.

## Estado actual (ya existe)

- `CategoryRemoteDataSource` — `getAll()`, `getById()` (solo lectura, **no tiene** método `create()`)
- `ProductRemoteDataSource` — `getAll()`, `getByCategory(categoryId)`, `search(query)`, `getBySeller(sellerId)`, `create()`, `update()`, `delete()`
- TensorFlow Lite 2.14.0 declarado en dependencias
- Directorio `ml/` vacío (solo `.gitignore`)
- `nav_graph.xml` con 7 destinos base (login, register, forgot, home, search, chat, profile)
- `MainActivity.kt` con toolbar + bottom nav + ocultar en auth screens
- Room 2.6.1 declarado

## Tareas

### 1. RecommendationEngine (TF Lite wrapper)

- **Ubicación**: `app/src/main/java/com/market/temues/ml/RecommendationEngine.kt`
- Este es el motor que Yami usará para rankear productos en el Home según las búsquedas del usuario

```kotlin
package com.market.temues.ml

import com.market.temues.model.Product
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Motor de recomendación que rankea productos según relevancia.
 * Para MVP usa un enfoque estadístico (TF-IDF simple sobre categorías y tags).
 * En futura versión puede delegar a un modelo TFLite.
 */
@Singleton
class RecommendationEngine @Inject constructor() {

    data class ScoredProduct(
        val product: Product,
        val score: Float
    )

    /**
     * Rankea productos según el historial de búsqueda del usuario.
     * @param products Lista completa de productos activos
     * @param searchHistory Términos de búsqueda recientes del usuario (máximo 50)
     * @return Productos ordenados por score descendente
     */
    fun rankProducts(
        products: List<Product>,
        searchHistory: List<String>
    ): List<Product> {
        if (searchHistory.isEmpty() || products.isEmpty()) {
            // Sin historial: devolver orden original (por fecha)
            return products
        }

        // Extraer palabras clave del historial
        val keywords = searchHistory
            .flatMap { it.lowercase().split(" ") }
            .filter { it.length > 2 }
            .groupingBy { it }
            .eachCount()

        if (keywords.isEmpty()) return products

        val maxKeywordFreq = keywords.values.max().toFloat()

        // Puntuar cada producto
        val scored = products.map { product ->
            val score = calculateScore(product, keywords, maxKeywordFreq)
            ScoredProduct(product, score)
        }

        // Ordenar por score descendente
        return scored
            .sortedByDescending { it.score }
            .map { it.product }
    }

    private fun calculateScore(
        product: Product,
        keywords: Map<String, Int>,
        maxFreq: Float
    ): Float {
        var score = 0f

        // Coincidencia en nombre (peso alto)
        val nameWords = product.name.lowercase().split(" ")
        for ((keyword, freq) in keywords) {
            if (nameWords.any { it.contains(keyword) }) {
                score += 3f * (freq / maxFreq)
            }
        }

        // Coincidencia en categoría (peso medio)
        val categoryLower = product.categoryName.lowercase()
        for ((keyword, freq) in keywords) {
            if (categoryLower.contains(keyword)) {
                score += 2f * (freq / maxFreq)
            }
        }

        // Coincidencia en tags (peso medio)
        for (tag in product.tags) {
            val tagLower = tag.lowercase()
            for ((keyword, freq) in keywords) {
                if (tagLower.contains(keyword)) {
                    score += 2f * (freq / maxFreq)
                }
            }
        }

        // Coincidencia en descripción (peso bajo)
        val descLower = product.description.lowercase()
        for ((keyword, freq) in keywords) {
            if (descLower.contains(keyword)) {
                score += 1f * (freq / maxFreq)
            }
        }

        return score
    }
}
```

### 2. Obtención y entrenamiento del modelo .tflite

Para que el `RecommendationEngine` use un modelo real de TensorFlow Lite en lugar del scoring estadístico, sigue estos pasos para crear y exportar el modelo desde Python.

#### 2.1. Script de entrenamiento (Python)

Crear archivo `training/recommendation_model.py` en la raíz del proyecto (o en cualquier máquina con Python):

```python
import tensorflow as tf
import numpy as np
import json
import os

# ------------------------------------------------------------
# 1. DEFINIR EL MODELO
# ------------------------------------------------------------
# Arquitectura: red neuronal densa que recibe un vector de
# características del producto + embedding del historial
# y predice un score de relevancia (0.0 - 1.0)

def build_model(input_dim):
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(64, activation='relu', input_shape=(input_dim,)),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(1, activation='sigmoid')  # score entre 0 y 1
    ])
    model.compile(
        optimizer='adam',
        loss='binary_crossentropy',
        metrics=['accuracy']
    )
    return model

# ------------------------------------------------------------
# 2. GENERAR DATOS DE ENTRENAMIENTO SINTÉTICOS
# ------------------------------------------------------------
# En producción se usan datos reales (búsquedas + clics).
# Aquí simulamos para demostrar el flujo.

def generate_synthetic_data(num_samples=5000):
    """
    Genera datos sintéticos simulando:
    - Características del producto (one-hot de categoría + tags + precio normalizado)
    - Score objetivo (1.0 si el usuario haría clic, 0.0 si no)
    """
    num_features = 50  # debe coincidir con el encoding real
    
    X = np.random.random((num_samples, num_features)).astype(np.float32)
    
    # Score sintético: combinación lineal + ruido
    weights = np.random.random(num_features).astype(np.float32)
    y = np.clip(X @ weights + np.random.normal(0, 0.1, num_samples), 0, 1)
    y = (y > 0.5).astype(np.float32)  # binarizar para clasificación
    
    return X, y

# ------------------------------------------------------------
# 3. ENTRENAR
# ------------------------------------------------------------
input_dim = 50
model = build_model(input_dim)

X_train, y_train = generate_synthetic_data(5000)
X_val, y_val = generate_synthetic_data(1000)

print("Entrenando modelo...")
history = model.fit(
    X_train, y_train,
    validation_data=(X_val, y_val),
    epochs=20,
    batch_size=32,
    verbose=1
)

# ------------------------------------------------------------
# 4. CONVERTIR A TFLITE
# ------------------------------------------------------------
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]  # cuantización para reducir tamaño
converter.target_spec.supported_types = [tf.float16]   # precisión media

tflite_model = converter.convert()

# Guardar el archivo .tflite
output_dir = "../app/src/main/assets"
os.makedirs(output_dir, exist_ok=True)
output_path = os.path.join(output_dir, "recommendation_model.tflite")
with open(output_path, 'wb') as f:
    f.write(tflite_model)

print(f"Modelo guardado en: {output_path}")
print(f"Tamaño: {len(tflite_model) / 1024:.1f} KB")
```

#### 2.2. Requisitos para entrenar (Python)

```bash
pip install tensorflow numpy
python training/recommendation_model.py
```

#### 2.3. Características de entrada (feature vector)

El modelo espera un vector de 50 floats. En `RecommendationEngine` (Android), debes construir ese vector para cada producto así:

| Índices | Característica | Descripción |
|---|---|---|
| 0–6 | Categoría (one-hot) | 7 categorías: electronica, ropa, hogar, deportes, vehiculos, servicios, otros |
| 7–16 | Tags (multi-hot) | 10 tags más comunes, 1 si el producto lo tiene |
| 17–26 | Keyword match (10) | 1 si el producto coincide con cada keyword del historial (top 10 keywords) |
| 27–36 | Precio normalizado | precio / max_precio (0.0–1.0) |
| 37 | Es nuevo | 1 si condition == "nuevo" |
| 38–49 | Reservado | 0.0 (para expansión futura) |

#### 2.4. Cargar y ejecutar el modelo en Android

Agregar el archivo `recommendation_model.tflite` en `app/src/main/assets/`.

Luego reemplazar el contenido de `RecommendationEngine` con la versión TFLite:

```kotlin
package com.market.temues.ml

import android.content.Context
import com.market.temues.model.Product
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationEngine @Inject constructor(
    private val context: Context
) {
    private var interpreter: Interpreter? = null
    private val maxPrice = 1000.0 // valor máximo esperado para normalizar

    init {
        try {
            val model = loadModelFile()
            interpreter = Interpreter(model)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("recommendation_model.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun rankProducts(products: List<Product>, searchHistory: List<String>): List<Product> {
        if (interpreter == null || searchHistory.isEmpty() || products.isEmpty()) {
            return products // fallback a orden original
        }

        // Extraer top keywords del historial
        val keywords = searchHistory
            .flatMap { it.lowercase().split(" ") }
            .filter { it.length > 2 }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }

        // Puntuar cada producto con el modelo TFLite
        return products.map { product ->
            val inputVector = buildFeatureVector(product, keywords)
            val output = Array(1) { FloatArray(1) }
            interpreter?.run(inputVector, output)
            Pair(product, output[0][0])
        }
        .sortedByDescending { it.second }
        .map { it.first }
    }

    private fun buildFeatureVector(product: Product, keywords: List<String>): Array<Array<FloatArray>> {
        val vector = FloatArray(50)

        // 0–6: One-hot categoría
        val categories = listOf("electronica", "ropa", "hogar", "deportes", "vehiculos", "servicios", "otros")
        val catIndex = categories.indexOf(product.categoryId)
        if (catIndex >= 0) vector[catIndex] = 1f

        // 7–16: Tags multi-hot (tags más comunes)
        val commonTags = listOf("apple", "samsung", "nike", "gamer", "usado", "nuevo", "laptop", "celular", "zapatos", "tv")
        for ((i, tag) in commonTags.withIndex()) {
            if (product.tags.any { it.lowercase() == tag }) {
                vector[7 + i] = 1f
            }
        }

        // 17–26: Keyword match del historial
        val productText = (product.name + " " + product.categoryName + " " + product.tags.joinToString(" ")).lowercase()
        for ((i, keyword) in keywords.withIndex()) {
            if (productText.contains(keyword)) {
                vector[17 + i] = 1f
            }
        }

        // 27: Precio normalizado
        vector[27] = (product.price / maxPrice).toFloat().coerceIn(0f, 1f)

        // 37: Es nuevo
        vector[37] = if (product.condition == "nuevo") 1f else 0f

        // 38–49: reservado (0)

        return arrayOf(arrayOf(vector))
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
```

#### 2.5. Notas importantes

- **Fallback automático**: si el archivo `.tflite` no existe o falla al cargar, `interpreter` será `null` y `rankProducts()` usará el orden original. La app nunca debe crashear por falta del modelo.
- **Datos reales**: los datos sintéticos del script Python son solo para demostración. En producción necesitas exportar datos reales de Firestore (búsquedas + clics + compras) como CSV, y entrenar con esos.
- **Exportar datos reales**: puedes usar una Firebase Function o descargar manualmente las colecciones `searchHistory` y `products` como JSON, convertirlas a vectors, y reentrenar.
- **Actualizar el modelo**: cuando tengas nuevos datos, reentrena en Python, reemplaza el `.tflite` en `assets/`, y la app usará el nuevo modelo automáticamente al reiniciar.

### 3. TrendAnalyzer (para admin dashboard)

- **Ubicación**: `app/src/main/java/com/market/temues/ml/TrendAnalyzer.kt`

```kotlin
@Singleton
class TrendAnalyzer @Inject constructor() {

    data class TrendResult(
        val categoryName: String,
        val productCount: Int,
        val trend: String  // "up", "stable", "down"
    )

    fun analyze(categoryCounts: Map<String, Int>): List<TrendResult> {
        if (categoryCounts.isEmpty()) return emptyList()
        val avgCount = categoryCounts.values.average()
        return categoryCounts.map { (name, count) ->
            val trend = when {
                count > avgCount * 1.2 -> "up"
                count < avgCount * 0.8 -> "down"
                else -> "stable"
            }
            TrendResult(name, count, trend)
        }.sortedByDescending { it.productCount }
    }
}
```

### 4. AdminDashboardViewModel + AdminDashboardFragment

#### AdminDashboardViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/admin/AdminDashboardViewModel.kt`
- Inyectar `ProductRemoteDataSource`, `CategoryRemoteDataSource`, `TrendAnalyzer`
- Recolectar datos en tiempo real: todos los productos, todas las categorías
- Derivar estadísticas:
  - `totalProducts: StateFlow<Int>` — conteo de productos activos
  - `productsByCategory: StateFlow<List<CategoryStat>>` — agrupar por `categoryName`
  - `recentProducts: StateFlow<List<Product>>` — últimos 5
  - `categoryTrends: StateFlow<List<TrendResult>>` — usando `TrendAnalyzer`
- Modelos de datos:
  ```kotlin
  data class CategoryStat(val categoryName: String, val count: Int, val percentage: Float)
  data class TrendResult(val categoryName: String, val productCount: Int, val trend: String)
  ```

#### AdminDashboardFragment

- **Ubicación**: `app/src/main/java/com/market/temues/ui/admin/AdminDashboardFragment.kt`
- **Layout**: `res/layout/fragment_admin_dashboard.xml`
- Toolbar: "Panel de Administración"
- Cards/Secciones:
  - **Resumen**: card con "Total de productos: XX"
  - **Productos por categoría**: lista con barras de progreso (nombre, barra, porcentaje)
  - **Tendencias**: lista de categorías con indicador ↑ stable ↓
  - **Productos recientes**: lista pequeños de últimos 5
- Botón "Crear categoría" → navegar a `adminCreateCategoryFragment`
- Estados loading, error, empty

### 5. Extender CategoryRemoteDataSource

Agregar método `create()` a `CategoryRemoteDataSource.kt`:

```kotlin
suspend fun create(category: Category): String {
    val docRef = collection.document()
    docRef.set(category.copy(id = docRef.id)).await()
    return docRef.id
}
```

### 6. AdminCreateCategoryFragment

#### AdminCreateCategoryViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/admin/AdminCreateCategoryViewModel.kt`
- Inyectar `CategoryRemoteDataSource`
- `fun createCategory(name: String, iconUrl: String, order: Int)`:
  - Validar nombre no vacío
  - Crear en Firestore
  - Emitir resultado

#### AdminCreateCategoryFragment

- **Ubicación**: `app/src/main/java/com/market/temues/ui/admin/AdminCreateCategoryFragment.kt`
- **Layout**: `res/layout/fragment_admin_create_category.xml`
- Toolbar: "Crear categoría"
- Formulario: nombre (obligatorio), URL ícono (opcional), orden (número, opcional)
- Botón "Guardar", al éxito Snackbar + navegar atrás

### 7. DatabaseModule (si no existe aún)

- **Ubicación**: `app/src/main/java/com/market/temues/di/DatabaseModule.kt`
- Si Otoniel o Ricardo no lo han creado, créalo tú para que Room funcione en la app
- Debe proveer `TemUESDatabase` con todas las entidades que existan

### 8. Utils — Helpers Globales

#### NetworkUtils

- **Ubicación**: `app/src/main/java/com/market/temues/utils/NetworkUtils.kt`

```kotlin
object NetworkUtils {
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

#### DateUtils

- **Ubicación**: `app/src/main/java/com/market/temues/utils/DateUtils.kt`

```kotlin
object DateUtils {
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "ahora"
            diff < 3_600_000 -> "${diff / 60_000} min"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            diff < 604_800_000 -> "${diff / 86_400_000}d"
            else -> formatTimestamp(timestamp)
        }
    }
}
```

#### ValidationUtils

- **Ubicación**: `app/src/main/java/com/market/temues/utils/ValidationUtils.kt`

```kotlin
object ValidationUtils {
    fun isValidPrice(price: String): Boolean =
        price.toDoubleOrNull()?.let { it > 0 } ?: false

    fun isNotEmpty(vararg fields: String?): Boolean =
        fields.all { !it.isNullOrBlank() }
}
```

### 9. Integración de Navegación Global (nav_graph.xml final)

Revisar y consolidar todos los destinos que el equipo agregue al `nav_graph.xml`:

#### Destinos esperados (además de los 7 existentes)

| ID | Fragment | Responsable |
|---|---|---|
| `productDetailFragment` | `ProductDetailFragment` | Yami |
| `cartFragment` | `CartFragment` | Otoniel |
| `checkoutFragment` | `CheckoutFragment` | Otoniel |
| `purchaseHistoryFragment` | `PurchaseHistoryFragment` | Otoniel |
| `chatDetailFragment` | `ChatDetailFragment` | Gaby |
| `sellerCatalogFragment` | `SellerCatalogFragment` | Ricardo |
| `addEditProductFragment` | `AddEditProductFragment` | Ricardo |
| `favoritesFragment` | `FavoritesFragment` | Ricardo |
| `editProfileFragment` | `EditProfileFragment` | Ricardo |
| `adminDashboardFragment` | `AdminDashboardFragment` | Javier |
| `adminCreateCategoryFragment` | `AdminCreateCategoryFragment` | Javier |

#### Acciones a verificar

- `homeFragment` → `productDetailFragment`, `cartFragment`
- `searchFragment` → `productDetailFragment`
- `productDetailFragment` → `cartFragment`, `checkoutFragment`, `chatDetailFragment`
- `cartFragment` → `checkoutFragment`
- `chatFragment` → `chatDetailFragment`
- `profileFragment` → `sellerCatalogFragment`, `favoritesFragment`, `editProfileFragment`, `purchaseHistoryFragment`, `adminDashboardFragment`
- `sellerCatalogFragment` → `addEditProductFragment`
- `favoritesFragment` → `productDetailFragment`
- `adminDashboardFragment` → `adminCreateCategoryFragment`

#### Resolver conflictos de merge

- Cada fragmento es independiente dentro de `<navigation>`
- Conflictos típicos: IDs duplicados, acciones duplicadas
- Mantener acciones ordenadas por ID de fragmento origen

### 10. Toolbar y Bottom Nav — Ajustes Finales

Verificar que `MainActivity.kt` oculte toolbar y bottom nav en los destinos correctos:

```kotlin
val authDestinations = setOf(
    R.id.loginFragment, R.id.registerFragment, R.id.forgotPasswordFragment
)
val fullScreenDestinations = setOf(
    R.id.chatDetailFragment, R.id.checkoutFragment, R.id.addEditProductFragment
)

navController.addOnDestinationChangedListener { _, destination, _ ->
    val hideAll = destination.id in authDestinations || destination.id in fullScreenDestinations
    binding.toolbar.isVisible = !hideAll
    binding.bottomNavigation.isVisible = destination.id !in authDestinations
}
```

### 11. Navegación — destinos de admin a agregar

```xml
<fragment
    android:id="@+id/adminDashboardFragment"
    android:name="com.market.temues.ui.admin.AdminDashboardFragment"
    android:label="@string/admin_dashboard_title" />
<fragment
    android:id="@+id/adminCreateCategoryFragment"
    android:name="com.market.temues.ui.admin.AdminCreateCategoryFragment"
    android:label="@string/admin_create_category_title" />
```

Acción desde `profileFragment` → `adminDashboardFragment` (condicional: solo admin)

### 12. Strings

```xml
<string name="admin_dashboard_title">Panel de Administración</string>
<string name="admin_create_category_title">Crear categoría</string>
<string name="admin_total_products">Total de productos</string>
<string name="admin_products_by_category">Productos por categoría</string>
<string name="admin_trends">Tendencias</string>
<string name="admin_recent_products">Productos recientes</string>
<string name="admin_create_category">Crear categoría</string>
<string name="admin_category_name">Nombre de la categoría</string>
<string name="admin_category_icon">URL del ícono</string>
<string name="admin_category_order">Orden</string>
<string name="admin_category_saved">Categoría creada</string>
<string name="trend_up">↑</string>
<string name="trend_stable">→</string>
<string name="trend_down">↓</string>
```

## Criterios de aceptación

- [ ] `RecommendationEngine` rankea productos según historial de búsqueda (Yami lo consume)
- [ ] Admin dashboard muestra total de productos, distribución por categoría y tendencias
- [ ] `TrendAnalyzer` clasifica tendencias como up/stable/down
- [ ] Admin puede crear nuevas categorías desde la app
- [ ] Todos los fragmentos de la app están declarados y conectados en el nav_graph
- [ ] Toolbar y bottom nav se comportan correctamente en todas las pantallas
- [ ] Los helpers globales (fechas, red, validación) están disponibles en `utils/`

## Dependencias

- TF Lite SDK ya declarado en el proyecto
- El resto del equipo debe tener sus fragments listos para la integración final del nav_graph
- Mientras tanto, trabajar en `RecommendationEngine`, admin dashboard, category creation y utils
