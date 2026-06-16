# Yami — Navegación y Descubrimiento de Productos

## Objetivo

Implementar el flujo principal de navegación de productos: explorar el listado en Home rankeado por relevancia (basado en búsquedas del usuario), buscar productos por texto y categoría, y ver el detalle de cada uno incluyendo punto de entrega.

## Estado actual (ya existe)

- `HomeFragment.kt` — stub vacío (solo infla `fragment_home.xml`)
- `SearchFragment.kt` — stub vacío
- `ProductRemoteDataSource` — `getAll()`, `search(query)`, `getById(id)`, `getByCategory(categoryId)`
- `CategoryRemoteDataSource` — `getAll()`
- `nav_graph.xml` — ya tiene los destinos `homeFragment` y `searchFragment`
- Layouts: `fragment_home.xml`, `fragment_search.xml` (placeholder)
- `RecommendationEngine` — lo crea Javier en `ml/`, tú lo inyectas y usas

## Tareas

### 1. HomeViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/home/HomeViewModel.kt`
- Inyectar `ProductRemoteDataSource`, `RecommendationEngine`, `FirebaseAuth`
- `sealed class ProductListUiState` con estados `Loading`, `Success(products)`, `Error(message)`
- `val uiState: StateFlow<ProductListUiState>`
- En `init`, recolectar `productRemoteDataSource.getAll()` y pasar a `recommendationEngine.rankProducts(products, uid)`
- `val rankedProducts: StateFlow<List<Product>>` — productos ordenados por score de recomendación
- Si `RecommendationEngine` no tiene datos suficientes, mostrar productos por fecha (orden original)

### 2. HomeFragment (mejorar el stub existente)

- Inyectar `HomeViewModel` con `by viewModels()`
- `RecyclerView` con `LinearLayoutManager` vertical
- Adapter: `ProductAdapter` con ViewBinding para `item_product_card.xml`
- Mostrar: imagen (Glide), nombre, precio, ubicación, condición
- Mostrar loading (`ProgressBar`), empty state ("No hay productos"), error state (Snackbar + retry)
- `setOnClickListener` por item → navegar a `productDetailFragment` con `productId` como argumento Safe Args
- Pull-to-refresh con `SwipeRefreshLayout`

### 3. ProductDetailViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/detail/ProductDetailViewModel.kt`
- Inyectar `ProductRemoteDataSource`
- Tomar `productId` como parámetro (SavedStateHandle)
- `val product: StateFlow<Product?>`
- Botones de acción (solo UI state por ahora):
  - Favorito → emitir evento `ToggleFavorite(productId)`
  - Chat → emitir evento `OpenChat(sellerId, productId)`
  - Carrito → emitir evento `AddToCart(productId)`
  - Comprar → emitir evento `BuyNow(productId)`

### 4. ProductDetailFragment

- **Ubicación**: `app/src/main/java/com/market/temues/ui/detail/ProductDetailFragment.kt`
- Layout: `res/layout/fragment_product_detail.xml`
- Mostrar:
  - Imagen principal grande (ImageView + Glide)
  - Nombre (TextView, título grande)
  - Precio (TextView, destacado)
  - Condición + Ubicación (chip/chips)
  - Descripción (TextView)
  - **Punto de entrega**: TextView con `product.location` si no es `"coordinar por chat"`, en ese caso mostrar "Coordinar por chat" con ícono de mensaje
  - Botones: ❤️ Favorito, 💬 Chat con vendedor, 🛒 Agregar al carrito, 💳 Comprar ahora
- Navegación desde botones:
  - Chat: navegar a `chatDetailFragment` con `sellerId` y `productId`
  - Carrito: navegar a `cartFragment`
  - Comprar: navegar a `checkoutFragment` con `productId`

### 5. SearchViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/search/SearchViewModel.kt`
- Inyectar `ProductRemoteDataSource`, `CategoryRemoteDataSource`, `FirebaseFirestore`, `FirebaseAuth`
- `val query = MutableStateFlow("")`
- `val selectedCategoryId = MutableStateFlow("")`
- `val categories: StateFlow<List<Category>>` — de `categoryRemoteDataSource.getAll()`
- `val results: StateFlow<List<Product>>` — combinar `query` y `selectedCategoryId` para llamar `search()` o `getByCategory()`
- Debounce de 300ms en la búsqueda
- **Guardar historial de búsqueda**: cada vez que el usuario presione buscar (o después de 300ms de inactividad), guardar el query en Firestore en `users/{uid}/searchHistory/{id}` con `query` y `timestamp`
- El formato del documento de búsqueda:
  ```kotlin
  data class SearchEntry(
      val query: String,
      val timestamp: Long = System.currentTimeMillis()
  )
  ```

### 6. SearchFragment (mejorar el stub existente)

- Campo de búsqueda (`SearchView` o `EditText` con `addTextChangedListener`)
- Chips horizontales de categorías (`RecyclerView` horizontal o `ChipGroup`)
- `RecyclerView` de resultados con mismo `ProductAdapter`
- Al hacer clic en un resultado → navegar a `productDetailFragment` con `productId`

### 7. Layout: item_product_card.xml

- `res/layout/item_product_card.xml`
- CardView o MaterialCardView
- ImageView (thumbnail), nombre, precio, ubicación, condición badge
- Aspect ratio ~1:1 para la imagen

### 8. Navegación (añadir a nav_graph.xml)

Agregar este destino (sin modificar los existentes):

```xml
<fragment
    android:id="@+id/productDetailFragment"
    android:name="com.market.temues.ui.detail.ProductDetailFragment"
    android:label="@string/product_detail_title">
    <argument
        android:name="productId"
        android:defaultValue="" />
</fragment>
```

Agregar acciones:
- Desde `homeFragment` a `productDetailFragment`
- Desde `searchFragment` a `productDetailFragment`

## Criterios de aceptación

- [ ] Home carga productos rankeados por relevancia según búsquedas del usuario
- [ ] Los productos se muestran en cards con imagen, nombre, precio, ubicación
- [ ] Al hacer clic en un producto se abre el detalle con toda la información
- [ ] Si `location == "coordinar por chat"` se muestra ese texto, si no el nombre de la ubicación
- [ ] Búsqueda funciona con debounce y filtro por categoría
- [ ] Las búsquedas del usuario se guardan en Firestore para alimentar el recomendador
- [ ] Los estados loading/empty/error se muestran correctamente

## Dependencias

- `RecommendationEngine` — lo provee Javier en `app/src/main/java/com/market/temues/ml/RecommendationEngine.kt`. Tú solo lo inyectas y llamas `rankProducts(products, userId)`.
