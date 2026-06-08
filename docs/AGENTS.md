# TemUES — Guía Rápida

## Requisitos

- Android Studio Hedgehog+ · JDK 21+ · Android SDK 35
- `google-services.json` en `app/` (está en `.gitignore`)

## Arquitectura

MVVM con Hilt + ViewBinding + Navigation Component.

```
com.market.temues/
├── TemUESApp.kt              @HiltAndroidApp
├── MainActivity.kt           @AndroidEntryPoint
├── di/                       Módulos Hilt
├── model/                    Data classes
├── data/
│   ├── local/                Room
│   ├── remote/               Firebase, Retrofit
│   └── repository/           AuthRepository
├── ui/auth/                  Login, registro, forgot pass
├── ui/home/                  HomeFragment
├── ui/search/                SearchFragment
├── ui/chat/                  ChatListFragment
├── ui/profile/               Perfil + cerrar sesión
├── ui/common/                Adapters
├── utils/                    Extensiones
└── ml/                       TensorFlow Lite
```

## Documentación

| Archivo | Contenido |
|---|---|
| [docs/architecture.md](architecture.md) | Arquitectura general y stack |
| [docs/authentication.md](authentication.md) | Flujo de autenticación completo |
| [docs/development-guide.md](development-guide.md) | Guía para crear nuevas funcionalidades |
| [docs/data-layer.md](data-layer.md) | Especificación Local vs Remoto por feature |
| [docs/libraries.md](libraries.md) | Referencia de todas las librerías |

## Convenciones

| Elemento | Convención | Ejemplo |
|---|---|---|
| Clases | PascalCase | `ProductRepository` |
| Funciones | camelCase | `loadProducts()` |
| Layouts / IDs / Strings | snake_case | `fragment_home.xml`, `btn_login`, `error_network` |

## ViewBinding (obligatorio)

```kotlin
private var _binding: FragmentXxxBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...): View {
    _binding = FragmentXxxBinding.inflate(inflater, container, false)
    return binding.root
}

override fun onDestroyView() { super.onDestroyView(); _binding = null }
```

## ViewModel + StateFlow

```kotlin
sealed class UiState { data object Loading; data class Success(...); data class Error(...) }

@HiltViewModel
class XxxViewModel @Inject constructor(
    private val repo: XxxRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```

## Observar en Fragment

```kotlin
viewModel.uiState.asLiveData().observe(viewLifecycleOwner) { state -> when (state) { ... } }
```

## Navegación

```kotlin
findNavController().navigate(R.id.action_login_to_home) // con Safe Args
```

## Hilt

```kotlin
@AndroidEntryPoint class LoginFragment : Fragment()
@HiltViewModel class AuthViewModel @Inject constructor(...)
@Singleton class AuthRepository @Inject constructor(...)
```

## Comandos

```bash
.\gradlew.bat assembleDebug           # Build debug
.\gradlew.bat test                    # Unit tests
.\gradlew.bat connectedAndroidTest    # Instrumented tests
.\gradlew.bat lint                    # Static analysis
```

## Git

```
main → develop → feature/*
feat: / fix: / refactor: / docs: / chore:
```

## Checklist antes de commit

- [ ] `assembleDebug` exitoso
- [ ] `test` pasa
- [ ] Sin strings hardcodeadas (usar `@string/`)
- [ ] Sin `findViewById` (usar ViewBinding)
- [ ] Lógica en ViewModel, no en Fragment/Activity
- [ ] Sin API keys ni secrets hardcodeados

