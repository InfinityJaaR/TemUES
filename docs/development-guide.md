# Guía de Desarrollo — Cómo Crear una Nueva Funcionalidad

## Flujo Paso a Paso

### 1. Modelo

Define la `data class` en `model/`:

```kotlin
package com.market.temues.model

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val sellerId: String = ""
)
```

### 2. Fuente de Datos

**Si es remoto (Firestore / Retrofit)** — en `data/remote/`:

```kotlin
class ProductRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getProducts(): Flow<Result<List<Product>>> = callbackFlow {
        firestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                } else {
                    val products = snapshot?.documents?.mapNotNull {
                        it.toObject(Product::class.java)
                    } ?: emptyList()
                    trySend(Result.success(products))
                }
            }
        awaitClose { }
    }
}
```

**Si es local (Room)** — en `data/local/`:

```kotlin
@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAll(): Flow<List<ProductEntity>>

    @Insert
    suspend fun insert(product: ProductEntity)
}
```

### 3. Repositorio

```kotlin
@Singleton
class ProductRepository @Inject constructor(
    private val remoteDataSource: ProductRemoteDataSource
) {
    fun getProducts(): Flow<Result<List<Product>>> =
        remoteDataSource.getProducts()
}
```

### 4. ViewModel

En la carpeta de la funcionalidad (ej. `ui/home/`):

```kotlin
sealed class ProductUiState {
    data object Loading : ProductUiState()
    data class Success(val products: List<Product>) : ProductUiState()
    data class Error(val message: String) : ProductUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductUiState>(ProductUiState.Loading)
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    init { loadProducts() }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = ProductUiState.Loading
            productRepository.getProducts().collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { ProductUiState.Success(it) },
                    onFailure = { ProductUiState.Error(it.message ?: "Error") }
                )
            }
        }
    }
}
```

### 5. Layout XML

Crear en `res/layout/fragment_home.xml` con ViewBinding.

### 6. Fragment

```kotlin
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.uiState.asLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProductUiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is ProductUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    // mostrar productos
                }
                is ProductUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### 7. Navegación

Registrar en `res/navigation/nav_graph.xml`:

```xml
<fragment
    android:id="@+id/homeFragment"
    android:name="com.market.temues.ui.home.HomeFragment"
    android:label="@string/nav_home"
    tools:layout="@layout/fragment_home">

    <action
        android:id="@+id/action_home_to_detail"
        app:destination="@id/productDetailFragment" />
</fragment>
```

Navegar con Safe Args:

```kotlin
findNavController().navigate(R.id.action_home_to_detail)
```

### 8. Strings

Agregar los textos en `res/values/strings.xml`:

```xml
<string name="home_title">Inicio - Descubre Productos</string>
<string name="product_price">Precio: $%s</string>
```

## Hilt — Añadir dependencias al proyecto

Si la nueva funcionalidad requiere una librería nueva:

1. **Agregar versión** en `gradle/libs.versions.toml`:
```toml
[versions]
nuevaLib = "1.0.0"

[libraries]
nueva-lib = { group = "com.example", name = "example-lib", version.ref = "nuevaLib" }
```

2. **Agregar dependencia** en `app/build.gradle.kts`:
```kotlin
implementation(libs.nueva.lib)
```

3. **Crear módulo Hilt** si la librería necesita ser inyectada:
```kotlin
@Module @InstallIn(SingletonComponent::class)
object NuevaLibModule {
    @Provides @Singleton
    fun provideNuevaLib(): NuevaLib = NuevaLib()
}
```

## Resumen del Flujo

```
1. Modelo (data class)
       ↓
2. Fuente de datos (remote/local)
       ↓
3. Repositorio
       ↓
4. ViewModel (StateFlow + sealed class UiState)
       ↓
5. Layout XML (ViewBinding)
       ↓
6. Fragment (@AndroidEntryPoint)
       ↓
7. Navegación (nav_graph + Safe Args)
       ↓
8. Strings (strings.xml)
```
