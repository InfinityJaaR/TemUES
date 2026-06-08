# Capa de Datos — Especificación por Negocio

TemUES es un **marketplace** donde compradores y vendedores interactúan. Aquí se define exactamente qué va en `data/local/`, `data/remote/` y `data/repository/`.

---

## Resumen

| Capa | Tecnología | Propósito |
|---|---|---|
| `data/local/` | **Room** | Cache offline, datos del usuario local, historial |
| `data/remote/` | **Firebase Auth** | Autenticación |
| `data/remote/` | **Firebase Firestore** | BD principal en la nube |
| `data/remote/` | **Firebase Storage** | Imágenes de productos y avatares |
| `data/remote/` | **Retrofit** | APIs externas (pagos, geocoding, etc.) |
| `data/remote/` | **Firebase Messaging** | Notificaciones push |
| `data/repository/` | **Clases Kotlin** | Orquestan local + remoto |

---

## data/local/ — Room Database

Base de datos local para **offline-first** y cache. Solo se almacena lo que el usuario necesita ver sin conexión o datos sensibles a latencia.

### Entidades (tablas)

| Entidad | Campos | Propósito |
|---|---|---|
| `ProductEntity` | `id` (PK), `name`, `description`, `price`, `categoryId`, `sellerId`, `imageUrl`, `condition`, `location`, `createdAt`, `updatedAt`, `isFavorite` | Cache de productos vistos recientemente |
| `CategoryEntity` | `id` (PK), `name`, `iconUrl`, `parentId`, `order` | Categorías para filtros offline |
| `ChatMessageEntity` | `id` (PK), `chatId`, `senderId`, `text`, `imageUrl`, `timestamp`, `isRead` | Mensajes del chat activo para acceso offline |
| `ChatEntity` | `id` (PK), `otherUserId`, `otherUserName`, `otherUserPhoto`, `lastMessage`, `lastMessageTimestamp`, `unreadCount` | Lista de conversaciones del usuario |
| `FavoriteEntity` | `id` (PK = productId), `productId`, `addedAt` | Productos favoritos (marcados por el usuario) |
| `SearchHistoryEntity` | `id` (PK auto), `query`, `timestamp` | Historial de búsquedas locales |
| `UserProfileEntity` | `id` (PK), `name`, `email`, `photoUrl`, `phone`, `bio`, `rating`, `memberSince` | Datos del perfil del usuario actual |

### DAOs

| DAO | Operaciones principales |
|---|---|
| `ProductDao` | `getAll()`, `getById()`, `search(query)`, `getByCategory()`, `insertAll()`, `deleteAll()` |
| `CategoryDao` | `getAll()`, `insertAll()`, `deleteAll()` |
| `ChatMessageDao` | `getByChatId(chatId)`, `insert()`, `markAsRead()`, `deleteByChatId()` |
| `ChatDao` | `getAll()`, `getById()`, `insert()`, `updateLastMessage()`, `delete()` |
| `FavoriteDao` | `getAll()`, `isFavorite(productId)`, `insert()`, `delete()` |
| `SearchHistoryDao` | `getRecent(limit)`, `insert()`, `deleteAll()` |
| `UserProfileDao` | `getById()`, `insert()`, `update()` |

### Database

```kotlin
@Database(
    entities = [
        ProductEntity::class, CategoryEntity::class,
        ChatMessageEntity::class, ChatEntity::class,
        FavoriteEntity::class, SearchHistoryEntity::class,
        UserProfileEntity::class
    ],
    version = 1
)
abstract class TemUESDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatDao(): ChatDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun userProfileDao(): UserProfileDao
}
```

---

## data/remote/ — Firebase & APIs

Capa que se comunica con servicios externos. NO contiene lógica de negocio, solo llamadas a la red.

### Firebase Auth (AuthDataSource)

| Método | Descripción |
|---|---|
| `loginWithEmail(email, pass)` | FirebaseAuth.signInWithEmailAndPassword |
| `registerWithEmail(email, pass)` | FirebaseAuth.createUserWithEmailAndPassword |
| `signInWithGoogle(idToken)` | GoogleAuthProvider.getCredential → signInWithCredential |
| `signInWithFacebook(token)` | FacebookAuthProvider.getCredential → signInWithCredential |
| `sendPasswordReset(email)` | FirebaseAuth.sendPasswordResetEmail |
| `signOut()` | FirebaseAuth.signOut |
| `getCurrentUser()` | FirebaseAuth.currentUser |

### Firebase Firestore (ProductDataSource, UserDataSource, ChatDataSource, etc.)

#### Colección: `products`

```kotlin
class ProductRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getAll(): Flow<List<Product>>             // .collection("products").addSnapshotListener
    fun getById(id: String): Flow<Product?>       // .document(id).addSnapshotListener
    fun getByCategory(categoryId: String): Flow<List<Product>>
    fun search(query: String): Flow<List<Product>>  // whereArrayContains("tags", query)
    fun getBySeller(sellerId: String): Flow<List<Product>>
    fun create(product: Product): suspend Unit      // .document().set()
    fun update(product: Product): suspend Unit
    fun delete(id: String): suspend Unit
}
```

**Documento `products/{productId}`**:
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "price": 0.0,
  "categoryId": "string",
  "sellerId": "string",
  "images": ["url1", "url2"],
  "condition": "nuevo | usado",
  "location": "string",
  "tags": ["tag1", "tag2"],
  "status": "activo | vendido | pausado",
  "createdAt": Timestamp,
  "updatedAt": Timestamp
}
```

#### Colección: `categories`

```kotlin
class CategoryRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getAll(): Flow<List<Category>>
}
```

**Documento `categories/{categoryId}`**:
```json
{
  "id": "string",
  "name": "string",
  "iconUrl": "string",
  "parentId": "string | null",
  "order": 0
}
```

#### Colección: `users`

```kotlin
class UserRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getById(id: String): Flow<User?>
    fun update(user: User): suspend Unit
}
```

**Documento `users/{userId}`**:
```json
{
  "id": "string",
  "email": "string",
  "name": "string",
  "photoUrl": "string",
  "phone": "string",
  "bio": "string",
  "rating": 0.0,
  "reviewCount": 0,
  "memberSince": Timestamp,
  "createdAt": Timestamp
}
```

#### Colección: `chats`

```kotlin
class ChatRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getUserChats(userId: String): Flow<List<Chat>>
    fun getOrCreateChat(user1Id: String, user2Id: String): Flow<Chat>
}
```

**Documento `chats/{chatId}`**:
```json
{
  "id": "string",
  "participants": ["userId1", "userId2"],
  "lastMessage": "string",
  "lastMessageTimestamp": Timestamp,
  "lastMessageSenderId": "string",
  "createdAt": Timestamp
}
```

#### Subcolección: `chats/{chatId}/messages`

```kotlin
class MessageRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getMessages(chatId: String): Flow<List<Message>>
    fun sendMessage(chatId: String, message: Message): suspend Unit
    fun markAsRead(chatId: String, messageId: String): suspend Unit
}
```

**Documento `chats/{chatId}/messages/{messageId}`**:
```json
{
  "id": "string",
  "chatId": "string",
  "senderId": "string",
  "text": "string",
  "imageUrl": "string | null",
  "timestamp": Timestamp,
  "isRead": false
}
```

### Firebase Storage (StorageDataSource)

```kotlin
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    fun uploadProductImage(productId: String, imageUri: Uri): Flow<Result<String>>
    fun uploadProfileImage(userId: String, imageUri: Uri): Flow<Result<String>>
    fun deleteImage(url: String): suspend Unit
}
```

### Retrofit — APIs Externas (opcional)

Si se necesitan APIs de terceros:

```kotlin
interface GeoApiService {
    @GET("maps/api/geocode/json")
    suspend fun geocode(@Query("address") address: String): GeocodeResponse
}

interface StripeApiService {
    @POST("payment-intents")
    suspend fun createPaymentIntent(@Body request: PaymentIntentRequest): PaymentIntentResponse
}
```

### Firebase Cloud Messaging (FCM)

Para notificaciones push cuando:
- Un usuario recibe un nuevo mensaje en el chat
- Un producto de un favorito cambia de precio
- Un vendedor recibe una nueva pregunta sobre un producto

---

## data/repository/ — Repositorios

Cada repositorio **orquesta** una fuente remota y una local. Deciden de dónde leer y cómo sincronizar.

### Repositorios necesarios

| Repositorio | Fuente Remota | Fuente Local | Estrategia |
|---|---|---|---|
| `AuthRepository` | Firebase Auth | — | Solo remoto (ya implementado) |
| `ProductRepository` | `ProductRemoteDataSource` | `ProductDao` | Remote primero, cache local para offline |
| `CategoryRepository` | `CategoryRemoteDataSource` | `CategoryDao` | Remote primero, cache local |
| `ChatRepository` | `ChatRemoteDataSource` + `MessageRemoteDataSource` | `ChatDao` + `ChatMessageDao` | Remote con cache offline |
| `UserRepository` | `UserRemoteDataSource` + Firebase Auth | `UserProfileDao` | Remote, cache perfil propio |
| `FavoriteRepository` | — | `FavoriteDao` | Solo local (sincronizar con Firestore después) |
| `SearchRepository` | — | `SearchHistoryDao` | Solo local |
| `StorageRepository` | `StorageDataSource` | — | Solo remoto |

### Ejemplo de estrategia (ProductRepository)

```kotlin
fun getProducts(categoryId: String?): Flow<Result<List<Product>>> = flow {
    // 1. Emitir datos cacheados primero (rápido)
    localDao.getByCategory(categoryId).collect { cached ->
        if (cached.isNotEmpty()) emit(Result.success(cached.toDomain()))
    }

    // 2. Emitir datos de Firestore (actualizado)
    remoteDataSource.getByCategory(categoryId).collect { remote ->
        localDao.insertAll(remote.toEntities())  // actualizar cache
        emit(Result.success(remote))
    }
}
```

---

## Mapa Completo: Feature → Data Layer

| Feature | Pantalla | Remote | Local | Repositorio |
|---|---|---|---|---|
| Login/Registro | auth/ | Firebase Auth | — | `AuthRepository` |
| Google Sign-In | auth/ | Firebase Auth + Firestore | — | `AuthRepository` |
| Facebook Login | auth/ | Firebase Auth + Firestore | — | `AuthRepository` |
| Ver productos | home/ | `ProductRemoteDataSource` | `ProductDao` | `ProductRepository` |
| Buscar productos | search/ | `ProductRemoteDataSource` | `ProductDao` + `SearchHistoryDao` | `ProductRepository` + `SearchRepository` |
| Categorías | home/ | `CategoryRemoteDataSource` | `CategoryDao` | `CategoryRepository` |
| Chat | chat/ | `ChatRemoteDataSource` + `MessageRemoteDataSource` | `ChatDao` + `ChatMessageDao` | `ChatRepository` |
| Perfil | profile/ | `UserRemoteDataSource` | `UserProfileDao` | `UserRepository` |
| Favoritos | profile/ | — (futuro: Firestore) | `FavoriteDao` | `FavoriteRepository` |
| Subir producto | — (futuro) | `ProductRemoteDataSource` + `StorageDataSource` | `ProductDao` | `ProductRepository` |
| Imágenes | — (futuro) | `StorageDataSource` | — | `StorageRepository` |
| Notificaciones | — (futuro) | FCM | — | — |
