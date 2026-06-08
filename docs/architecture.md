# Arquitectura del Proyecto — TemUES

## Ficha Técnica

| Propiedad | Valor |
|---|---|
| Package | `com.market.temues` |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |
| Kotlin | 1.9.22 |
| AGP | 8.3.2 |

## Patrón Arquitectónico: MVVM

```
View (Fragment/Activity)
    ↓ Observa (asLiveData / collect)
ViewModel (StateFlow / LiveData)
    ↓ Llama
Repository
    ↓ Accede a
RemoteDataSource (Firebase)  ──  LocalDataSource (Room)
```

- **Model**: Data classes + lógica de negocio
- **ViewModel**: Puente entre Vista y datos. Expone `StateFlow`. Sobrevive a cambios de configuración
- **View**: Fragments/Activities. Solo muestra datos y reacciona a interacciones. Sin lógica de decisión

## Estructura de Paquetes

```
com.market.temues/
├── TemUESApp.kt              @HiltAndroidApp
├── MainActivity.kt           @AndroidEntryPoint
├── di/                       Módulos Hilt (FirebaseModule)
├── model/                    Data classes (User.kt)
├── data/
│   ├── local/                Room — cache offline [docs/data-layer.md](data-layer.md)
│   │   ├── TemUESDatabase.kt
│   │   ├── dao/              ProductDao, CategoryDao, ChatDao, etc.
│   │   └── entity/           ProductEntity, CategoryEntity, etc.
│   ├── remote/               Firebase + Retrofit [docs/data-layer.md](data-layer.md)
│   │   ├── auth/             Firebase Auth
│   │   ├── product/          ProductRemoteDataSource
│   │   ├── chat/             ChatRemoteDataSource, MessageRemoteDataSource
│   │   ├── user/             UserRemoteDataSource
│   │   ├── storage/          StorageDataSource
│   │   └── api/              Retrofit interfaces (GeoApiService, etc.)
│   └── repository/           AuthRepository, ProductRepository, etc.
├── ui/
│   ├── auth/                 Login, registro, forgot pass
│   ├── home/                 HomeFragment (placeholder)
│   ├── search/               SearchFragment (placeholder)
│   ├── chat/                 ChatListFragment (placeholder)
│   ├── profile/              Perfil + cerrar sesión
│   └── common/               Adapters, componentes reutilizables
├── utils/                    Extensiones, formateadores
└── ml/                       TensorFlow Lite
```

## Stack Tecnológico

| Capa | Tecnología | Documentación |
|---|---|---|
| Autenticación | Firebase Auth (email, Google, Facebook) | [docs/authentication.md](authentication.md) |
| Base de datos cloud | Firebase Firestore | [docs/authentication.md](authentication.md) |
| Almacenamiento | Firebase Storage | _próximamente_ |
| Inyección de dependencias | Hilt 2.50 | [docs/development-guide.md](development-guide.md#hilt) |
| Navegación | Navigation Component 2.7.7 con Safe Args | [docs/development-guide.md](development-guide.md#navegación) |
| Persistencia local | Room 2.6.1 (KSP) | [docs/libraries.md](libraries.md#room) |
| Networking | Retrofit 2.9.0 + Gson | [docs/libraries.md](libraries.md#retrofit) |
| Cámara | CameraX 1.3.1 | [docs/libraries.md](libraries.md#camerax) |
| Pagos | Stripe 20.37.0 | [docs/libraries.md](libraries.md#stripe) |
| ML | TensorFlow Lite 2.14.0 | [docs/libraries.md](libraries.md#tensorflow-lite) |
| Imágenes | Glide 4.16.0 (KSP) | [docs/libraries.md](libraries.md#glide) |
| Animaciones | Lottie 6.3.0 | [docs/libraries.md](libraries.md#lottie) |

## Documentación Relacionada

| Archivo | Contenido |
|---|---|
| [docs/authentication.md](authentication.md) | Flujo completo de autenticación |
| [docs/development-guide.md](development-guide.md) | Cómo crear una nueva funcionalidad paso a paso |
| [docs/libraries.md](libraries.md) | Referencia de todas las librerías con ejemplos |
| [docs/AGENTS.md](AGENTS.md) | Guía rápida, convenciones, comandos |
