# Ricardo — Catálogo del Vendedor, Favoritos Offline y Edición de Perfil

## Objetivo

Permitir que los vendedores gestionen sus productos (CRUD completo), que los usuarios guarden productos favoritos con soporte offline (Room + Firestore), y que editen su perfil (nombre, teléfono, bio; la foto se toma de Google si está disponible).

## Estado actual (ya existe)

- `ProfileFragment.kt` — funcionalidad básica (muestra email + logout con Google Sign-Out)
- `ProductRemoteDataSource` — `getBySeller(sellerId)`, `create(product)`, `update(product)`, `delete(id)`
- `UserRemoteDataSource` — `getById(id)`, `save(user)`, `update(id, data)`
- `StorageDataSource` — `uploadProductImage(imageUri)`, `uploadAvatar(userId, imageUri)`, `deleteImage(url)`
- Modelo `User` con campos `id`, `email`, `name`, `photoUrl`, `createdAt`
- Modelo `Product` con campos completos (name, description, price, categoryId, categoryName, sellerId, sellerName, images, condition, location, tags, status, createdAt, updatedAt)
- Directorios `data/local/entity/` y `data/local/dao/` vacíos (Room 2.6.1 ya declarado)
- `nav_graph.xml` tiene `profileFragment` con acción `action_profile_to_login`
- CameraX declarado (opcional para cámara en formulario)
- Glide declarado para carga de imágenes
- Google Sign-In ya integrado (foto disponible en `user.photoUrl`)

## Tareas

### 1. SellerCatalogViewModel + SellerCatalogFragment

#### SellerCatalogViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/seller/SellerCatalogViewModel.kt`
- Inyectar `ProductRemoteDataSource`, `FirebaseAuth`
- Obtener `currentUser.uid`
- `val products: StateFlow<List<Product>>` — de `productRemoteDataSource.getBySeller(uid)`
- `sealed class CatalogUiState { Loading, Success(products), Error(message), Empty }`
- `fun deleteProduct(productId: String)` — llamar `productRemoteDataSource.delete(productId)`
- `fun toggleStatus(productId: String, currentStatus: String)` — actualizar campo `status` a `"activo"` o `"pausado"`

#### SellerCatalogFragment

- **Ubicación**: `app/src/main/java/com/market/temues/ui/seller/SellerCatalogFragment.kt`
- **Layout**: `res/layout/fragment_seller_catalog.xml`
- Toolbar: "Mis productos"
- RecyclerView con cards de producto del vendedor:
  - Imagen (Glide), nombre, precio, estado (badge verde "Activo" o naranja "Pausado")
  - Botones: Editar (lápiz), Eliminar (papelera), Toggle estado (Activar/Pausar)
- Confirmación con AlertDialog antes de eliminar
- FAB (FloatingActionButton) para agregar producto nuevo → navegar a `addEditProductFragment` sin `productId`
- Al hacer clic en editar → navegar a `addEditProductFragment` con `productId`
- Estados: loading, empty ("Aún no tienes productos. ¡Crea tu primer anuncio!"), error

### 2. AddEditProductViewModel + AddEditProductFragment

#### AddEditProductViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/seller/AddEditProductViewModel.kt`
- Inyectar `ProductRemoteDataSource`, `CategoryRemoteDataSource`, `StorageDataSource`, `FirebaseAuth`
- Modo creación o edición según si recibe `productId` (SavedStateHandle)
- Si es edición: cargar producto existente con `productRemoteDataSource.getById(productId)`
- Campos observables (MutableStateFlow):
  - `name`, `description`, `price`, `condition` ("nuevo" o "usado"), `categoryId`, `location`, `tags`
- `val categories: StateFlow<List<Category>>` — de `categoryRemoteDataSource.getAll()`
- `val images: MutableStateFlow<List<String>>` — URLs de imágenes
- `uploadImage(uri: Uri)`:
  - Llamar `StorageDataSource.uploadProductImage(uri)`
  - Al obtener URL exitosa, agregar a `images`
- `removeImage(index: Int)` — eliminar URL de la lista
- `fun save()`:
  - Validar campos obligatorios (nombre, precio, categoría)
  - Si es nuevo: `productRemoteDataSource.create(product)` con `sellerId = uid`, `sellerName` del usuario
  - Si es edición: `productRemoteDataSource.update(product)`
  - Emitir evento `SaveSuccess` o `SaveError`

#### AddEditProductFragment

- **Ubicación**: `app/src/main/java/com/market/temues/ui/seller/AddEditProductFragment.kt`
- **Layout**: `res/layout/fragment_add_edit_product.xml`
- ScrollView con formulario:
  - **Nombre**: EditText, obligatorio
  - **Descripción**: EditText multilinea
  - **Precio**: EditText con inputType `numberDecimal`, obligatorio
  - **Condición**: RadioGroup con opciones "Nuevo" y "Usado"
  - **Categoría**: Spinner o RecyclerView con categorías del ViewModel
  - **Ubicación**: EditText (nombre del punto de entrega: "Mi casa", "Trabajo", "Coordinar por chat")
  - **Tags**: ChipGroup con EditText para agregar tags personalizados
  - **Imágenes**: Grid de imágenes (2 columnas), cada una con botón de eliminar, más un botón "Agregar foto" que abre selector de galería o cámara
- Botón "Guardar" al final
- Validación visual: campos obligatorios con error si están vacíos al guardar
- Al guardar exitosamente: navegar hacia atrás (popBackStack)

### 3. Favoritos — Modelo Room

#### FavoriteEntity

- **Ubicación**: `app/src/main/java/com/market/temues/data/local/entity/FavoriteEntity.kt`

```kotlin
package com.market.temues.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "productId"],
    indices = [Index("userId"), Index("productId")]
)
data class FavoriteEntity(
    val userId: String,
    val productId: String,
    val productName: String = "",
    val productPrice: Double = 0.0,
    val productImage: String = "",
    val isSynced: Boolean = false, // control offline -> online sync
    val createdAt: Long = System.currentTimeMillis()
)
```

#### FavoriteDao

- **Ubicación**: `app/src/main/java/com/market/temues/data/local/dao/FavoriteDao.kt`

```kotlin
package com.market.temues.data.local.dao

import androidx.room.*
import com.market.temues.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAll(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND productId = :productId)")
    fun isFavorite(userId: String, productId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND productId = :productId)")
    suspend fun isFavoriteSync(userId: String, productId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND productId = :productId")
    suspend fun delete(userId: String, productId: String)

    @Query("SELECT * FROM favorites WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsynced(userId: String): List<FavoriteEntity>

    @Query("UPDATE favorites SET isSynced = 1 WHERE userId = :userId AND productId = :productId")
    suspend fun markAsSynced(userId: String, productId: String)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun clearAll(userId: String)
}
```

#### TemUESDatabase (actualizar si ya existe)

- **Ubicación**: `app/src/main/java/com/market/temues/data/local/TemUESDatabase.kt`
- Si Otoniel ya creó `TemUESDatabase` con `CartEntity`:
  - Agregar `FavoriteEntity` a la lista de entidades
  - Incrementar version (ej: de 1 a 2)
  - Usar `fallbackToDestructiveMigration()` para desarrollo
- Si no existe: crear `TemUESDatabase` con ambas entidades

### 4. FavoritesRepository (Sync offline/online)

- **Ubicación**: `app/src/main/java/com/market/temues/data/repository/FavoritesRepository.kt`

```kotlin
@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val firestore: FirebaseFirestore
) {
    private fun userFavoritesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("favorites")

    fun getAll(userId: String): Flow<List<FavoriteEntity>> = favoriteDao.getAll(userId)
    fun isFavorite(userId: String, productId: String): Flow<Boolean> = favoriteDao.isFavorite(userId, productId)

    suspend fun addFavorite(userId: String, product: Product) {
        val fav = FavoriteEntity(
            userId = userId,
            productId = product.id,
            productName = product.name,
            productPrice = product.price,
            productImage = product.images.firstOrNull() ?: "",
            isSynced = false
        )
        favoriteDao.insert(fav)
        try {
            userFavoritesCollection(userId).document(product.id).set(fav).await()
            favoriteDao.markAsSynced(userId, product.id)
        } catch (_: Exception) { }
    }

    suspend fun removeFavorite(userId: String, productId: String) {
        favoriteDao.delete(userId, productId)
        try {
            userFavoritesCollection(userId).document(productId).delete().await()
        } catch (_: Exception) { }
    }

    suspend fun toggleFavorite(userId: String, product: Product) {
        if (favoriteDao.isFavoriteSync(userId, product.id)) {
            removeFavorite(userId, product.id)
        } else {
            addFavorite(userId, product)
        }
    }

    suspend fun syncPendingFavorites(userId: String) {
        val unsynced = favoriteDao.getUnsynced(userId)
        for (fav in unsynced) {
            try {
                userFavoritesCollection(userId).document(fav.productId).set(fav).await()
                favoriteDao.markAsSynced(userId, fav.productId)
            } catch (_: Exception) { }
        }
    }
}
```

### 5. FavoritesViewModel + FavoritesFragment

#### FavoritesViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/favorites/FavoritesViewModel.kt`
- Inyectar `FavoritesRepository`, `FirebaseAuth`
- `val favorites: StateFlow<List<FavoriteEntity>>` — de `favoritesRepository.getAll(uid)`
- `fun removeFavorite(productId: String)`

#### FavoritesFragment

- **Ubicación**: `app/src/main/java/com/market/temues/ui/favorites/FavoritesFragment.kt`
- **Layout**: `res/layout/fragment_favorites.xml`
- RecyclerView con cards de productos favoritos: imagen, nombre, precio, botón quitar favorito
- Al hacer clic → navegar a `productDetailFragment` con `productId`
- Modo offline: mostrar indicador "Mostrando datos sin conexión"
- Estados: loading, empty ("No tienes favoritos"), error

### 6. EditProfileViewModel + EditProfileFragment

#### EditProfileViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/profile/EditProfileViewModel.kt`
- Inyectar `UserRemoteDataSource`, `StorageDataSource`, `FirebaseAuth`
- Cargar datos del usuario actual (`currentUser` de FirebaseAuth + `UserRemoteDataSource`)
- `val currentUser: StateFlow<User?>`
- `val saveState: StateFlow<SaveState>` — Idle, Saving, Success, Error
- `fun updateProfile(name: String, phone: String, bio: String)`:
  - Construir mapa de datos a actualizar
  - Llamar `userRemoteDataSource.update(uid, data)`
- **Foto de perfil**:
  - Verificar si `currentUser.photoUrl` ya existe (de Google Sign-In)
  - Si existe: emitir estado `HasGooglePhoto(url)` para que la UI deshabilite la edición
  - Si no existe: permitir subir foto con `StorageDataSource.uploadAvatar(uid, uri)`
  - `fun updatePhoto(uri: Uri)` — solo disponible si no hay Google photo

#### EditProfileFragment

- **Ubicación**: `app/src/main/java/com/market/temues/ui/profile/EditProfileFragment.kt`
- **Layout**: `res/layout/fragment_edit_profile.xml`
- Toolbar: "Editar perfil"
- ImageView circular para foto de perfil:
  - **Si hay Google photo**: mostrar la foto con un badge "Google" y deshabilitar cambio
  - **Si no hay**: mostrar placeholder, al hacer clic abrir selector galería/cámara
- Nombre: EditText (prellenado)
- Teléfono: EditText con inputType `phone`
- Bio: EditText multilinea
- Botón "Guardar cambios"
- Al guardar exitosamente: Snackbar y navegar hacia atrás

### 7. ProfileFragment (mejorar)

- **Ubicación**: `app/src/main/java/com/market/temues/ui/profile/ProfileFragment.kt`
- **Layout**: `res/layout/fragment_profile.xml` (mejorar)
- Sección de perfil: foto circular (Glide), nombre, email
- Lista de opciones (MaterialCardView o lista):
  - "Mis productos" → `sellerCatalogFragment`
  - "Favoritos" → `favoritesFragment`
  - "Editar perfil" → `editProfileFragment`
  - "Historial de compras" → `purchaseHistoryFragment`
  - "Panel de administración" → `adminDashboardFragment` (solo si `user.isAdmin == true`)
  - "Cerrar sesión" (ya existe)

### 8. User model — agregar campos faltantes

```kotlin
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 9. Navegación (añadir a nav_graph.xml)

```xml
<fragment
    android:id="@+id/sellerCatalogFragment"
    android:name="com.market.temues.ui.seller.SellerCatalogFragment"
    android:label="@string/seller_catalog_title" />
<fragment
    android:id="@+id/addEditProductFragment"
    android:name="com.market.temues.ui.seller.AddEditProductFragment"
    android:label="@string/add_edit_product_title">
    <argument android:name="productId" android:defaultValue="" />
</fragment>
<fragment
    android:id="@+id/favoritesFragment"
    android:name="com.market.temues.ui.favorites.FavoritesFragment"
    android:label="@string/favorites_title" />
<fragment
    android:id="@+id/editProfileFragment"
    android:name="com.market.temues.ui.profile.EditProfileFragment"
    android:label="@string/edit_profile_title" />
```

Acciones a agregar:
- `profileFragment` → `sellerCatalogFragment`
- `profileFragment` → `favoritesFragment`
- `profileFragment` → `editProfileFragment`
- `sellerCatalogFragment` → `addEditProductFragment` (sin argumento = crear nuevo)
- `sellerCatalogFragment` → `addEditProductFragment` (con `productId` = editar)
- `favoritesFragment` → `productDetailFragment` (con `productId`)

### 10. Strings

```xml
<string name="seller_catalog_title">Mis productos</string>
<string name="add_edit_product_title">Nuevo producto</string>
<string name="edit_product_title">Editar producto</string>
<string name="favorites_title">Favoritos</string>
<string name="edit_profile_title">Editar perfil</string>
<string name="seller_catalog_empty">Aún no tienes productos. ¡Crea tu primer anuncio!</string>
<string name="favorites_empty">No tienes productos favoritos</string>
<string name="product_add">Agregar producto</string>
<string name="product_save">Guardar</string>
<string name="product_delete_confirm">¿Eliminar este producto?</string>
<string name="product_status_active">Activo</string>
<string name="product_status_paused">Pausado</string>
<string name="product_condition_new">Nuevo</string>
<string name="product_condition_used">Usado</string>
<string name="profile_save">Guardar cambios</string>
<string name="profile_photo_change">Cambiar foto</string>
<string name="profile_photo_google">Foto de Google</string>
<string name="profile_phone">Teléfono</string>
<string name="profile_bio">Biografía</string>
<string name="favorites_offline_mode">Mostrando datos sin conexión</string>
```

## Criterios de aceptación

- [ ] Vendedor puede ver, crear, editar y eliminar sus productos
- [ ] El toggle activo/pausado se refleja en tiempo real en Firestore
- [ ] Favoritos se guardan localmente (Room) y se sincronizan con Firestore
- [ ] Sin internet, los favoritos se ven desde Room (offline-first)
- [ ] Al recuperar internet, los favoritos pendientes se sincronizan automáticamente
- [ ] Perfil muestra nombre, foto (de Google si existe) y email
- [ ] Si hay Google photo, se muestra y no se permite cambiar; si no, se puede subir foto
- [ ] Se puede editar nombre, teléfono y bio
- [ ] Los campos loading/error/empty se muestran en cada pantalla

## Dependencias

- Room 2.6.1 ya declarado. Si Otoniel ya creó `TemUESDatabase`, agrégale tus entidades (incrementando version). Si no, créala tú.
- `StorageDataSource` ya existe para subir imágenes.
- Google Sign-In ya integrado — `currentUser.photoUrl` trae la foto de Google automáticamente.
