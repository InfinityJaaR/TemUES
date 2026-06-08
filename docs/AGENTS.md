# TemUES - Guía de Desarrollo para el Equipo

## 📋 Requisitos Previos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17+ (Gradle lo auto-provisiona vía foojay)
- Android SDK 35
- Conexión a Firebase (google-services.json)

## 🚀 Primeros Pasos

```bash
git clone <repo-url>
cd TemUES
```

1. Coloca tu archivo `google-services.json` en `app/` (está en `.gitignore`)
2. Abre el proyecto en Android Studio
3. Sincroniza Gradle
4. Ejecuta en un emulador o dispositivo

## 🏗️ Arquitectura

MVVM con Hilt + ViewBinding + Navigation Component.

```
com.market.temues/
├── TemUESApp.kt              # @HiltAndroidApp
├── MainActivity.kt           # @AndroidEntryPoint
├── model/                    # Data classes (Product, User, Message, Category)
├── data/
│   ├── local/                # Room DB, DAOs, Entities
│   ├── remote/               # Firebase services, Retrofit interfaces
│   └── repository/           # Repositorios
├── ui/
│   ├── auth/                 # Login / Registro
│   ├── home/                 # HomeFragment + HomeViewModel
│   ├── search/               # SearchFragment + SearchViewModel
│   ├── chat/                 # ChatListFragment + ChatViewModel
│   ├── profile/              # ProfileFragment + ProfileViewModel
│   └── common/               # Adapters, componentes reutilizables
├── utils/                    # Extensiones, formateadores, permisos
└── ml/                       # TensorFlow Lite
```

## ✍️ Convenciones de Código

### Naming

| Elemento | Convención | Ejemplo |
|---|---|---|
| Clases | PascalCase | `HomeFragment`, `ProductRepository` |
| Funciones | camelCase | `loadProducts()`, `onViewCreated()` |
| Variables | camelCase | `binding`, `productList` |
| Archivos XML | snake_case | `fragment_home.xml`, `item_product.xml` |
| Recursos ID | snake_case | `@+id/btn_submit`, `@+id/rv_products` |
| Strings | snake_case | `home_title`, `error_network` |

### ViewBinding

Siempre usar ViewBinding. En fragments:

```kotlin
private var _binding: FragmentHomeBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...): View {
    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    return binding.root
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
}
```

### Hilt

- Activities/Fragments → `@AndroidEntryPoint`
- ViewModels → `@HiltViewModel` + `@Inject constructor`
- Repositorios/DataSources → `@Inject constructor` o `@Module`

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel()
```

### ViewModel + StateFlow

```kotlin
private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
```

## 🌿 Git Workflow

```text
main        → estable, listo para release
develop     → integración de features
feature/*   → feature/login, feature/search-screen
fix/*       → fix/crash-on-empty-state
```

- Commits en español o inglés (consistente)
- Prefijo: `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`

## 🧪 Tests

```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
./gradlew lint                    # Static analysis
```

## ✅ Checklist Antes de Commit

- [ ] Build exitoso (`./gradlew assembleDebug`)
- [ ] Tests pasan (`./gradlew test`)
- [ ] Sin strings hardcodeadas (usar `@string/`)
- [ ] Sin `findViewById` (usar ViewBinding)
- [ ] Sin lógica de negocio en Fragments/Activities
- [ ] Sin API keys o secrets hardcodeados
