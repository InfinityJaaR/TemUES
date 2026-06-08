# Librerías del Proyecto

## Firebase (BOM 32.7.2)

Todas las versiones se gestionan mediante el **Firebase BoM** — no se especifican versiones individuales.

| Artefacto | Propósito |
|---|---|
| `firebase-auth` | Autenticación |
| `firebase-firestore` | Base de datos NoSQL |
| `firebase-storage` | Almacenamiento de archivos |
| `firebase-messaging` | Notificaciones push (FCM) |
| `firebase-crashlytics` | Reporte de crashes |
| `firebase-analytics` | Analítica |

### FirebaseModule

`di/FirebaseModule.kt` — Provee `FirebaseAuth` y `FirebaseFirestore` vía Hilt.

```kotlin
@Module @InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
```

### Firestore — Uso típico

```kotlin
// Guardar
firestore.collection("users").document(user.id).set(user)

// Leer
firestore.collection("products").addSnapshotListener { snapshot, error ->
    snapshot?.documents?.map { it.toObject(Product::class.java) }
}
```

---

## Hilt (2.50)

Inyección de dependencias basada en Dagger.

### Reglas

| Dónde | Anotación |
|---|---|
| `Application` | `@HiltAndroidApp` (`TemUESApp`) |
| `Activity` | `@AndroidEntryPoint` |
| `Fragment` | `@AndroidEntryPoint` |
| `ViewModel` | `@HiltViewModel` + `@Inject constructor` |
| `Repository` | `@Inject constructor` + `@Singleton` |
| Clases externas | `@Module` + `@Provides` |

### Inyección en ViewModel

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel()
```

### Inyección en Fragment

```kotlin
@AndroidEntryPoint
class LoginFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()
}
```

---

## Navigation Component (2.7.7)

### Safe Args

Las acciones se definen en `res/navigation/nav_graph.xml`:

```xml
<action
    android:id="@+id/action_login_to_home"
    app:destination="@id/homeFragment"
    app:popUpTo="@id/loginFragment"
    app:popUpToInclusive="true" />
```

### Navegar en Fragment

```kotlin
findNavController().navigate(R.id.action_login_to_home)
```

### Bottom Nav — Ocultar en auth

En `MainActivity`:
```kotlin
val authDestinations = setOf(R.id.loginFragment, R.id.registerFragment, R.id.forgotPasswordFragment)
navController.addOnDestinationChangedListener { _, destination, _ ->
    val isAuthScreen = destination.id in authDestinations
    binding.toolbar.isVisible = !isAuthScreen
    binding.bottomNavigation.isVisible = !isAuthScreen
}
```

---

## Room (2.6.1)

Persistencia local SQLite con **KSP**.

```kotlin
// build.gradle.kts
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)
```

```kotlin
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAll(): Flow<List<ProductEntity>>

    @Insert
    suspend fun insert(product: ProductEntity)
}
```

---

## Retrofit (2.9.0) + Gson (2.10.1)

Cliente HTTP REST.

```kotlin
interface ApiService {
    @GET("products")
    suspend fun getProducts(): List<Product>
}

// proveer con Hilt:
@Provides @Singleton
fun provideRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

---

## CameraX (1.3.1)

```kotlin
implementation(libs.androidx.camera.core)
implementation(libs.androidx.camera.camera2)
implementation(libs.androidx.camera.lifecycle)
implementation(libs.androidx.camera.view)
```

Componentes: `PreviewView`, `ImageCapture`, `ImageAnalysis`.

---

## Stripe (20.37.0)

Procesamiento de pagos.

```kotlin
implementation(libs.stripe)
```

---

## TensorFlow Lite (2.14.0)

Modelos de ML para recomendaciones.

```kotlin
implementation(libs.tensorflow.lite)
```

---

## Glide (4.16.0)

Carga de imágenes con **KSP**.

```kotlin
implementation(libs.glide)
ksp(libs.glide.compiler)

// Uso:
Glide.with(fragment).load(url).into(imageView)
```

---

## Lottie (6.3.0)

Animaciones vectoriales JSON.

```kotlin
implementation(libs.lottie)
```

```xml
<com.airbnb.lottie.LottieAnimationView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:lottie_rawRes="@raw/animacion"
    app:lottie_autoPlay="true"
    app:lottie_loop="true" />
```

---

## Google Sign-In (20.7.0)

```kotlin
implementation(libs.play.services.auth)
```

### Flujo

```kotlin
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken(getString(R.string.default_web_client_id))
    .requestEmail()
    .build()
val googleSignInClient = GoogleSignIn.getClient(activity, gso)
val signInIntent = googleSignInClient.signInIntent
googleSignInLauncher.launch(signInIntent)
```

---

## Facebook Login (18.2.3)

```kotlin
implementation(libs.facebook.login)
```

Requiere en `AndroidManifest.xml`:
```xml
<meta-data android:name="com.facebook.sdk.ApplicationId"
    android:value="@string/facebook_app_id" />
```

Y registrar la app en [developers.facebook.com](https://developers.facebook.com).
