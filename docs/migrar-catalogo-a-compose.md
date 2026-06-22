# Migrar Catálogo de Productos del Vendedor a Jetpack Compose

> Basado en el patrón usado en `CategoryListFragment` (categorías).

## Resumen

Reemplazar el `RecyclerView` + `Adapter` actual del catálogo de productos del vendedor
(`SellerCatalogFragment`) por un `ComposeView` con `LazyColumn` + `SwipeToDismissBox`,
manteniendo el Toolbar, FAB y loading en XML.

Las imágenes se cargan con el **Glide que ya tiene el proyecto**, sin agregar nuevas dependencias.

---

## Estado actual vs. Estado final

| Capa | Antes | Después |
|---|---|---|
| Layout | `RecyclerView` + `item_seller_product.xml` | `ComposeView` en XML |
| Fragment | `Adapter` + `AlertDialog` de AppCompat | `setContent { }` + `AlertDialog` de Compose |
| Imágenes | `Glide.with().into(imageView)` | `Glide.asBitmap()` + `CustomTarget` + `Image(bitmap)` |
| ViewModel | `StateFlow<EstadoCatalogoUI>` (bien) | Sin cambios |
| Borrables | — | `SellerProductAdapter.kt`, `item_seller_product.xml` |

---

## 1. Modificar `fragment_seller_catalog.xml`

Reemplazar el `<RecyclerView>` por un `<ComposeView>` y corregir el id del Lottie:

```xml
<!-- ANTES -->
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rv_products"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    ... />

<!-- DESPUÉS -->
<androidx.compose.ui.platform.ComposeView
    android:id="@+id/composeView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

También renombrar el id del Lottie de `progress_bar` a `lottieLoading` para consistencia.

### Layout final completo

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/temues_bg">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/white">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:title="@string/seller_catalog_title"
            app:titleCentered="true" />
    </com.google.android.material.appbar.AppBarLayout>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <androidx.compose.ui.platform.ComposeView
            android:id="@+id/composeView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

        <com.airbnb.lottie.LottieAnimationView
            android:id="@+id/lottieLoading"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:visibility="gone"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:lottie_autoPlay="true"
            app:lottie_loop="true"
            app:lottie_rawRes="@raw/animacion_carga" />

        <LinearLayout
            android:id="@+id/layout_empty"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:orientation="vertical"
            android:padding="32dp"
            android:visibility="gone"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/seller_catalog_empty"
                android:textAlignment="center"
                android:textAppearance="@style/TextAppearance.Material3.BodyLarge" />
        </LinearLayout>
    </androidx.constraintlayout.widget.ConstraintLayout>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab_add_product"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:contentDescription="@string/product_add"
        app:srcCompat="@android:drawable/ic_input_add" />
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

---

## 2. Reescribir `SellerCatalogFragment.kt`

Eliminar la lógica de RecyclerView y usar `ComposeView.setContent { }`.

### Imports a agregar

```kotlin
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
```

### Imports a eliminar

```kotlin
import androidx.recyclerview.widget.LinearLayoutManager
import com.market.temues.databinding.ItemSellerProductBinding
import com.market.temues.ui.seller.SellerProductAdapter
```

### Observar estado del ViewModel

```kotlin
modelo.estadoUI.asLiveData().observe(viewLifecycleOwner) { estado ->
    binding.lottieLoading.isVisible = estado is EstadoCatalogoUI.Cargando
    binding.layoutEmpty.isVisible = estado is EstadoCatalogoUI.Vacio

    when (estado) {
        is EstadoCatalogoUI.Exito -> {
            binding.composeView.isVisible = true
            binding.composeView.setContent {
                SellerCatalogContent(
                    productos = estado.productos,
                    onEdit = { producto ->
                        val accion = SellerCatalogFragmentDirections
                            .actionSellerCatalogToAddEditProduct(producto.id)
                        findNavController().navigate(accion)
                    },
                    onDelete = { modelo.eliminarProducto(it.id) },
                    onToggleStatus = { modelo.cambiarEstado(it.id, it.status) }
                )
            }
        }
        is EstadoCatalogoUI.Error -> {
            binding.composeView.isVisible = false
            // Mostrar Snackbar o tvError
        }
        else -> binding.composeView.isVisible = false
    }
}
```

### Composable del listado

```kotlin
@Composable
private fun SellerCatalogContent(
    productos: List<Product>,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onToggleStatus: (Product) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(productos, key = { it.id }) { producto ->
            SwipeableProductItem(
                producto = producto,
                onEdit = onEdit,
                onDelete = onDelete,
                onToggleStatus = onToggleStatus
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
```

### Composable del item con SwipeToDismiss

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableProductItem(
    producto: Product,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onToggleStatus: (Product) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirm = true
                false
            } else {
                false
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                scope.launch { dismissState.reset() }
            },
            title = { Text("Eliminar producto") },
            text = { Text("¿Estás seguro de eliminar \"${producto.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(producto)
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch { dismissState.reset() }
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFFF44336),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Eliminar",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        ProductCard(
            producto = producto,
            onEdit = onEdit,
            onToggleStatus = onToggleStatus
        )
    }
}
```

### Composable de la tarjeta del producto

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(
    producto: Product,
    onEdit: (Product) -> Unit,
    onToggleStatus: (Product) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductImage(
                    url = producto.images.firstOrNull(),
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = producto.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format("%.2f", producto.price)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (producto.status == "activo") "Activo" else "Vendido",
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onToggleStatus(producto) }) {
                    Text(
                        if (producto.status == "activo")
                            "Marcar como vendido"
                        else
                            "Reactivar"
                    )
                }
                IconButton(onClick = { onEdit(producto) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
```

### Composable `ProductImage` — carga con Glide (sin nuevas dependencias)

```kotlin
@Composable
private fun ProductImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(url) {
        val target = object : CustomTarget<Bitmap>() {
            override fun onResourceReady(
                resource: Bitmap,
                transition: Transition<in Bitmap>?
            ) {
                bitmap.value = resource
            }
            override fun onLoadCleared(placeholder: Drawable?) {
                bitmap.value = null
            }
        }
        Glide.with(context)
            .asBitmap()
            .load(url)
            .into(target)
        onDispose { Glide.with(context).clear(target) }
    }

    Image(
        bitmap = bitmap.value?.asImageBitmap(),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}
```

---

## 3. Cambios en `SellerCatalogViewModel.kt`

Ya usa `StateFlow` con `EstadoCatalogoUI` — está bien. Solo agregar recarga tras eliminar para que la lista se refresque automáticamente:

```kotlin
fun eliminarProducto(idProducto: String) {
    viewModelScope.launch {
        try {
            fuenteRemotaProducto.delete(idProducto)
            cargarProductos() // Refrescar lista
        } catch (e: Exception) {
            _estadoUI.value = EstadoCatalogoUI.Error(
                e.message ?: "Error al eliminar"
            )
        }
    }
}
```

---

## 4. Archivos a eliminar

| Archivo | Motivo |
|---|---|
| `app/src/main/java/com/market/temues/ui/seller/SellerProductAdapter.kt` | Reemplazado por Compose |
| `app/src/main/res/layout/item_seller_product.xml` | Reemplazado por Compose inline |

---

## 5. Dependencias — sin cambios

No se agrega ninguna dependencia nueva. Las imágenes se cargan con el **Glide** que ya tiene el proyecto.

---

## Resumen de cambios por archivo

| Archivo | Acción |
|---|---|
| `fragment_seller_catalog.xml` | `RecyclerView` → `ComposeView`; Lottie id unificado |
| `SellerCatalogFragment.kt` | Eliminar adapter; agregar `setContent { }` + composables + Glide wrapper |
| `SellerCatalogViewModel.kt` | Recargar lista tras eliminar |
| `SellerProductAdapter.kt` | **Eliminar** |
| `item_seller_product.xml` | **Eliminar** |

---

## Comportamiento final

- ✅ Lista de productos con imagen, nombre, precio y estado
- ✅ Swipe izquierdo revela fondo rojo "Eliminar" con confirmación
- ✅ Botones Editar y Cambiar estado visibles siempre en la tarjeta
- ✅ FAB para agregar producto (XML)
- ✅ Lottie animación mientras carga (XML)
- ✅ Estado vacío con mensaje (XML)
- ✅ Toolbar "Mis productos" (XML)
- ✅ Sin dependencias nuevas — imágenes con Glide existente
