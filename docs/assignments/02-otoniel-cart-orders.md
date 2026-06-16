# Otoniel — Carrito, Checkout, Órdenes e Historial de Compras

## Objetivo

Implementar el flujo completo de compra: agregar productos al carrito (persistente en Room), pagar (efectivo o tarjeta con Stripe), generar orden en Firestore con código de recogida, notificar al vendedor por FCM, y mostrar historial de compras.

## Estado actual (ya existe)

- Room 2.6.1 declarado en dependencias (runtime, compiler, ktx, ksp)
- Stripe SDK declarado en dependencias
- Directorios `data/local/entity/` y `data/local/dao/` vacíos (solo `.gitignore`)
- `ProductRemoteDataSource`, `UserRemoteDataSource` existentes
- `nav_graph.xml` listo para agregar nuevos destinos
- Glide declarado para cargar imágenes
- FCM (`firebase.messaging`) declarado en dependencias

## Tareas

### 1. Modelo Order

- **Ubicación**: `app/src/main/java/com/market/temues/model/Order.kt`

```kotlin
package com.market.temues.model

data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val total: Double = 0.0,
    val paymentMethod: String = "", // "cash" o "card"
    val status: String = "pendiente", // pendiente, confirmado, entregado, cancelado
    val code: String = "", // código de 4 dígitos para recoger
    val sellerId: String = "", // para notificar al vendedor
    val deliveryLocation: String = "", // nombre del lugar de entrega (product.location)
    val createdAt: Long = System.currentTimeMillis()
)

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
)
```

### 2. OrderRemoteDataSource

- **Ubicación**: `app/src/main/java/com/market/temues/data/remote/order/OrderRemoteDataSource.kt`
- Inyectar `FirebaseFirestore`
- `collection = firestore.collection("orders")`
- `suspend fun create(order: Order): String` — crea documento, devuelve ID
- `fun getUserOrders(userId: String): Flow<List<Order>>` — snapshot listener filtrando por `userId`, ordenado por `createdAt` DESC
- `fun getSellerOrders(sellerId: String): Flow<List<Order>>` — para que el vendedor vea órdenes de sus productos
- `suspend fun updateStatus(orderId: String, status: String)`

### 3. Room — Entidad, DAO y Base de Datos

#### CartEntity

- **Ubicación**: `app/src/main/java/com/market/temues/data/local/entity/CartEntity.kt`

```kotlin
package com.market.temues.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart")
data class CartEntity(
    @PrimaryKey val productId: String,
    val productName: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int = 1,
    val sellerId: String = "", // para notificar al vendedor después
    val deliveryLocation: String = "" // lugar de entrega del producto
)
```

#### CartDao

- **Ubicación**: `app/src/main/java/com/market/temues/data/local/dao/CartDao.kt`

```kotlin
package com.market.temues.data.local.dao

import androidx.room.*
import com.market.temues.data.local.entity.CartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart")
    fun getAll(): Flow<List<CartEntity>>

    @Query("SELECT * FROM cart WHERE productId = :productId")
    suspend fun getItem(productId: String): CartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: CartEntity)

    @Query("DELETE FROM cart WHERE productId = :productId")
    suspend fun delete(productId: String)

    @Query("DELETE FROM cart")
    suspend fun clearAll()

    @Query("SELECT SUM(price * quantity) FROM cart")
    fun getTotalPrice(): Flow<Double?>
}
```

#### TemUESDatabase

- **Ubicación**: `app/src/main/java/com/market/temues/data/local/TemUESDatabase.kt`

```kotlin
package com.market.temues.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.market.temues.data.local.dao.CartDao
import com.market.temues.data.local.entity.CartEntity

@Database(entities = [CartEntity::class], version = 1, exportSchema = false)
abstract class TemUESDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
}
```

#### DatabaseModule (Hilt)

- **Ubicación**: `app/src/main/java/com/market/temues/di/DatabaseModule.kt`

```kotlin
package com.market.temues.di

import android.content.Context
import androidx.room.Room
import com.market.temues.data.local.TemUESDatabase
import com.market.temues.data.local.dao.CartDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TemUESDatabase =
        Room.databaseBuilder(context, TemUESDatabase::class.java, "temues_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCartDao(db: TemUESDatabase): CartDao = db.cartDao()
}
```

> **Nota**: Si otra persona necesita agregar más entidades a `TemUESDatabase` después, incrementen la versión y usen `fallbackToDestructiveMigration()`.

### 4. CartViewModel + CartFragment

#### CartViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/cart/CartViewModel.kt`
- Anotación `@HiltViewModel`
- Inyectar `CartDao`, `ProductRemoteDataSource`
- `val items: StateFlow<List<CartEntity>>` — de `cartDao.getAll()`
- `val total: StateFlow<Double>` — de `cartDao.getTotalPrice()` (mapear null a 0.0)
- `fun addToCart(product: Product, quantity: Int = 1)` — crear o actualizar `CartEntity` con `sellerId` y `deliveryLocation`
- `fun removeFromCart(productId: String)` — llamar `cartDao.delete()`
- `fun updateQuantity(productId: String, quantity: Int)` — obtener item y actualizar
- `fun clearCart()` — llamar `cartDao.clearAll()`

#### CartFragment

- **Ubicación**: `app/src/main/java/com/market/temues/ui/cart/CartFragment.kt`
- **Layout**: `res/layout/fragment_cart.xml`
- RecyclerView con items del carrito: imagen (Glide), nombre, precio unitario, cantidad (+/− botones), total por item, botón eliminar
- Footer fijo con total general y botón "Ir a pagar"
- Si el carrito está vacío: mostrar empty state con mensaje y botón "Explorar productos"
- Al presionar "Ir a pagar" → navegar a `checkoutFragment`

### 5. CheckoutViewModel + CheckoutFragment

#### CheckoutViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/checkout/CheckoutViewModel.kt`
- Inyectar `CartDao`, `OrderRemoteDataSource`, `FirebaseAuth`, `FirebaseFirestore`
- `val items: StateFlow<List<CartEntity>>` — de `cartDao.getAll()`
- `val total: StateFlow<Double>` — de `cartDao.getTotalPrice()`
- `val selectedPaymentMethod = MutableStateFlow("cash")` — "cash" o "card"
- `val orderResult = MutableStateFlow<OrderResult?>(null)`
- `sealed class OrderResult { data object Success : OrderResult(); data class Error(val message: String) : OrderResult() }`
- `fun confirmOrder()`:
  - Generar código aleatorio de 4 dígitos
  - Obtener `sellerId` del primer item (todos del mismo vendedor)
  - Obtener `deliveryLocation` del primer item
  - Crear `Order` con items, total, método de pago, código, `sellerId`, `deliveryLocation`
  - Llamar `orderRemoteDataSource.create(order)`
  - **Notificar al vendedor**: enviar FCM al `sellerId` con datos de la orden (total, código, mensaje "¡Tienes una nueva venta!")
  - Si éxito: limpiar carrito con `cartDao.clearAll()`, emitir `OrderResult.Success`
  - Si error: emitir `OrderResult.Error`

#### Stripe Integration (para pago con tarjeta)

- Si el método es "card":
  - Inicializar `Stripe(requireContext(), "pk_test_...")` con tu clave publicable
  - Usar `PaymentSheet` de Stripe para recolectar datos de tarjeta
  - Para MVP sin backend: simular PaymentIntent exitoso
  - Después de payment exitoso, proceder con `confirmOrder()`

#### Notificación FCM al vendedor

- Después de crear la orden, construir un mapa de datos FCM:
  ```kotlin
  val data = mapOf(
      "type" to "new_order",
      "orderId" to order.id,
      "orderCode" to order.code,
      "total" to order.total.toString(),
      "sellerId" to order.sellerId,
      "buyerId" to currentUserId
  )
  ```
- Enviar usando Firebase Functions o directamente con `FirebaseFirestore` (se puede crear un documento en `notifications/{sellerId}/messages/` que un servicio FCM consuma)
- Alternativa simple: crear un documento en `users/{sellerId}/notifications/{id}` con la info de la venta, y el `FirebaseMessagingService` de Gaby puede consumir estas notificaciones

#### CheckoutFragment

- **Ubicación**: `app/src/main/java/com/market/temues/ui/checkout/CheckoutFragment.kt`
- **Layout**: `res/layout/fragment_checkout.xml`
- Resumen de la orden: lista de items (solo lectura) + total
- **Punto de entrega**: mostrar `deliveryLocation` del producto (TextView, si es "coordinar por chat" mostrar ese texto)
- Selección de método de pago: `RadioGroup` con opciones "Efectivo" y "Tarjeta de crédito/débito"
- Si tarjeta: mostrar vista de Stripe `PaymentSheet`
- Botón "Confirmar pedido"
- Después de confirmar exitosamente: mostrar diálogo o pantalla con el código de recogida de 4 dígitos y botón "Ver mis compras"

### 6. PurchaseHistoryViewModel + PurchaseHistoryFragment

#### PurchaseHistoryViewModel

- **Ubicación**: `app/src/main/java/com/market/temues/ui/history/PurchaseHistoryViewModel.kt`
- Inyectar `OrderRemoteDataSource`, `FirebaseAuth`
- `val orders: StateFlow<List<Order>>` — de `orderRemoteDataSource.getUserOrders(uid)`
- `sealed class HistoryUiState { Loading, Success(orders), Error(message), Empty }`

#### PurchaseHistoryFragment

- **Ubicación**: `app/src/main/java/com/market/temues/ui/history/PurchaseHistoryFragment.kt`
- **Layout**: `res/layout/fragment_purchase_history.xml`
- RecyclerView con cards de orden: código de recogida, total, fecha (formateada), estado (badge de color), punto de entrega, cantidad de items
- Estados loading, empty ("No has realizado compras"), error

### 7. Vista del vendedor (opcional, si alcanza)

- Crear `SellerOrdersFragment` donde el vendedor ve las órdenes de sus productos
- Botón para cambiar estado: "Marcar como entregado"
- Similar a `PurchaseHistoryFragment` pero filtrando por `sellerId`

### 8. Navegación (añadir a nav_graph.xml)

```xml
<fragment
    android:id="@+id/cartFragment"
    android:name="com.market.temues.ui.cart.CartFragment"
    android:label="@string/cart_title" />
<fragment
    android:id="@+id/checkoutFragment"
    android:name="com.market.temues.ui.checkout.CheckoutFragment"
    android:label="@string/checkout_title" />
<fragment
    android:id="@+id/purchaseHistoryFragment"
    android:name="com.market.temues.ui.history.PurchaseHistoryFragment"
    android:label="@string/purchase_history_title" />
```

Acciones a agregar:
- `homeFragment` → `cartFragment`
- `productDetailFragment` → `cartFragment`
- `productDetailFragment` → `checkoutFragment`
- `cartFragment` → `checkoutFragment`
- `profileFragment` → `purchaseHistoryFragment`

### 9. Strings (agregar a strings.xml)

```xml
<string name="cart_title">Carrito</string>
<string name="checkout_title">Pagar</string>
<string name="purchase_history_title">Mis Compras</string>
<string name="cart_empty">Tu carrito está vacío</string>
<string name="cart_explore">Explorar productos</string>
<string name="checkout_total">Total</string>
<string name="checkout_cash">Efectivo</string>
<string name="checkout_card">Tarjeta</string>
<string name="checkout_confirm">Confirmar pedido</string>
<string name="order_code">Código de recogida</string>
<string name="order_placed">Pedido realizado con éxito</string>
<string name="order_delivery_location">Punto de entrega</string>
<string name="order_coordinate_by_chat">Coordinar por chat</string>
<string name="purchase_history_empty">No has realizado compras aún</string>
<string name="order_status_pending">Pendiente</string>
<string name="order_status_confirmed">Confirmado</string>
<string name="order_status_delivered">Entregado</string>
<string name="order_status_cancelled">Cancelado</string>
```

## Criterios de aceptación

- [ ] Productos se pueden agregar/quitar del carrito (persistente en Room)
- [ ] El total se actualiza automáticamente al cambiar cantidades
- [ ] Checkout permite elegir entre efectivo y tarjeta (Stripe)
- [ ] Stripe procesa pago con tarjeta correctamente (o simula éxito para MVP)
- [ ] Al confirmar se genera orden en Firestore con código único de 4 dígitos
- [ ] Se muestra el punto de entrega en checkout y en la orden
- [ ] El vendedor recibe notificación FCM cuando alguien compra su producto
- [ ] El carrito se limpia después de confirmar la orden
- [ ] Historial de compras muestra todas las órdenes del usuario en tiempo real
- [ ] Estados loading/empty/error se muestran en cada pantalla

## Dependencias

- Room 2.6.1 ya declarado
- Stripe SDK ya declarado. Necesitas clave publicable de Stripe (`pk_test_...`)
- FCM (`firebase.messaging`) ya declarado — `FirebaseMessagingService` lo crea Gaby, tú solo necesitas enviar la notificación escribiendo en una colección de Firestore o usando Firebase Functions
