# Configuración de Firestore — TemUES

> **Prerrequisito**: Haber habilitado **Cloud Firestore** en Firebase Console (modo de prueba o producción).

---

## 1. Security Rules

Ir a Firebase Console → Firestore → **Rules** y pegar lo siguiente:

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;

      match /favorites/{productId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }

    match /products/{productId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null
                    && request.resource.data.sellerId == request.auth.uid;
      allow update, delete: if request.auth != null
                            && resource.data.sellerId == request.auth.uid;
    }

    match /categories/{categoryId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null; // solo escritura para desarrollo (seeder)
    }

    match /chats/{chatId} {
      allow read, update: if request.auth != null
                           && request.auth.uid in resource.data.participants;
      allow create: if request.auth != null
                    && request.auth.uid in request.resource.data.participants;

      match /messages/{messageId} {
        allow read: if request.auth != null
                    && request.auth.uid in get(
                      /databases/$(database)/documents/chats/$(chatId)
                    ).data.participants;
        allow create: if request.auth != null
                      && request.auth.uid == request.resource.data.senderId;
        allow update: if request.auth != null
                      && request.auth.uid == resource.data.senderId
                      && request.resource.data.diff(resource.data)
                           .affectedKeys().hasOnly(["isRead"]);
      }
    }

    match /_meta/{document} {
      allow read, write: if request.auth != null;
    }
  }
}
```

> **Explicación**:
> - `users` — cualquier autenticado lee, solo su dueño escribe
> - `products` — cualquier autenticado lee, solo el vendedor crea/modifica/elimina
> - `categories` — cualquier autenticado lee/escribe (para desarrollo con seeder)
> - `chats` — solo los participantes leen/escriben
> - `messages` — solo participantes leen (verifica contra el padre `chats/{chatId}`), solo el sender crea, solo marca `isRead`
> - `_meta` — cualquier autenticado lee/escribe (para flags internos como el seeder)
>
> ⚠️ **Todas las reglas son por colección, NO por grupo de colecciones**.
> Los mensajes se acceden vía `chats/{chatId}/messages/{messageId}`, no con `collectionGroup`.
> Si en el futuro se necesitara búsqueda global de mensajes, habría que agregar
> `match /{path=**}/messages/{messageId}` adicional.
>
> ⚠️ **Producción**: Cambiar `categories` a `allow write: if false;` cuando ya no se necesite el seeder.

---

## 2. Índices compuestos (estructurados)

Son índices **compuestos** (no vectoriales). En Firebase Console → Firestore → **Indexes** → **Add index** y crear:

| # | Colección | Campos | Orden |
|---|---|---|---|
| 1 | `products` | `status` ↑, `createdAt` ↓ | Productos activos más recientes |
| 2 | `products` | `categoryId` ↑, `status` ↑, `createdAt` ↓ | Filtrar por categoría |
| 3 | `products` | `sellerId` ↑, `createdAt` ↓ | Productos de un vendedor |
| 4 | `products` | `status` ↑, `name` ↑ | Búsqueda por nombre + status activo |
| 5 | `chats` | `participants` ↑, `lastMessageTimestamp` ↓ | Conversaciones del usuario |

> **Nota**: El #4 es compuesto porque la query combina `whereEqualTo("status", "activo")` + `orderBy("name")`. No es un índice de un solo campo.

---

## 3. Estructura de Colecciones

### `users/{userId}`

| Campo | Tipo | Ejemplo |
|---|---|---|
| id | string | `"abc123uid"` |
| email | string | `"usuario@mail.com"` |
| name | string | `"Juan Pérez"` |
| photoUrl | string | `"https://..."` |
| phone | string | `"503 7000-0000"` |
| bio | string | `"Vendedor de electrónicos"` |
| rating | number | `4.5` |
| reviewCount | number | `12` |
| memberSince | timestamp | `June 8, 2026` |
| createdAt | timestamp | `June 8, 2026` |

### `users/{userId}/favorites/{productId}`

| Campo | Tipo | Ejemplo |
|---|---|---|
| productId | string | `"prod123"` |
| addedAt | timestamp | `June 8, 2026` |

### `products/{productId}`

| Campo | Tipo | Ejemplo |
|---|---|---|
| id | string | `"prod123"` |
| name | string | `"iPhone 13 Pro"` |
| description | string | `"En excelente estado..."` |
| price | number | `450.00` |
| categoryId | string | `"electronica"` |
| categoryName | string | `"Electrónicos"` |
| sellerId | string | `"abc123uid"` |
| sellerName | string | `"Juan Pérez"` |
| images | array<string> | `["url1", "url2"]` |
| condition | string | `"nuevo"` o `"usado"` |
| location | string | `"San Salvador"` |
| tags | array<string> | `["iphone", "apple", "celular"]` |
| status | string | `"activo"` o `"vendido"` o `"pausado"` |
| createdAt | timestamp | |
| updatedAt | timestamp | |

### `categories/{categoryId}`

| Campo | Tipo | Ejemplo |
|---|---|---|
| id | string | `"electronica"` |
| name | string | `"Electrónicos"` |
| iconUrl | string | `"https://..."` |
| parentId | string (null) | `null` |
| order | number | `1` |

### `chats/{chatId}`

| Campo | Tipo | Ejemplo |
|---|---|---|
| id | string | `"chat123"` |
| participants | array<string> | `["uid1", "uid2"]` |
| productId | string | `"prod123"` |
| productName | string | `"iPhone 13 Pro"` |
| productImage | string | `"https://..."` |
| lastMessage | string | `"Sí, está disponible"` |
| lastMessageTimestamp | timestamp | |
| lastMessageSenderId | string | `"uid2"` |
| createdAt | timestamp | |

### `chats/{chatId}/messages/{messageId}`

| Campo | Tipo | Ejemplo |
|---|---|---|
| id | string | `"msg456"` |
| chatId | string | `"chat123"` |
| senderId | string | `"uid1"` |
| text | string | `"Hola, ¿sigue disponible?"` |
| imageUrl | string | `""` |
| timestamp | timestamp | |
| isRead | boolean | `false` |

---

## 4. Datos de Prueba — Categorías

Insertar en Firebase Console → Firestore → `categories`:

```javascript
// categories/electronica
{ id: "electronica", name: "Electrónicos", iconUrl: "", parentId: null, order: 1 }

// categories/ropa
{ id: "ropa", name: "Ropa y Accesorios", iconUrl: "", parentId: null, order: 2 }

// categories/hogar
{ id: "hogar", name: "Hogar y Muebles", iconUrl: "", parentId: null, order: 3 }

// categories/deportes
{ id: "deportes", name: "Deportes y Ocio", iconUrl: "", parentId: null, order: 4 }

// categories/vehiculos
{ id: "vehiculos", name: "Vehículos", iconUrl: "", parentId: null, order: 5 }

// categories/servicios
{ id: "servicios", name: "Servicios", iconUrl: "", parentId: null, order: 6 }

// categories/otros
{ id: "otros", name: "Otros", iconUrl: "", parentId: null, order: 7 }
```

---

## 5. Firebase Storage Rules

Ir a Firebase Console → Storage → **Rules**:

```javascript
rules_version = '2';

service firebase.storage {
  match /b/{bucket}/o {
    match /products/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null
                   && request.resource.size < 10 * 1024 * 1024;
    }
    match /avatars/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null
                   && request.resource.size < 5 * 1024 * 1024;
    }
  }
}
```

---

## 6. DataSources implementados en el proyecto

| Clase | Colección | Archivo |
|---|---|---|
| `ProductRemoteDataSource` | `products` | `data/remote/product/ProductRemoteDataSource.kt` |
| `CategoryRemoteDataSource` | `categories` | `data/remote/category/CategoryRemoteDataSource.kt` |
| `ChatRemoteDataSource` | `chats` | `data/remote/chat/ChatRemoteDataSource.kt` |
| `MessageRemoteDataSource` | `chats/{id}/messages` | `data/remote/chat/MessageRemoteDataSource.kt` |
| `UserRemoteDataSource` | `users` | `data/remote/user/UserRemoteDataSource.kt` |
| `StorageDataSource` | Firebase Storage | `data/remote/storage/StorageDataSource.kt` |

---

## 7. Checklist de configuración

- [ ] Cloud Firestore habilitado en Firebase Console
- [ ] Security Rules copiadas y publicadas
- [ ] Índices compuestos creados
- [ ] Categorías de prueba insertadas
- [ ] Firebase Storage habilitado y reglas copiadas
- [ ] `google-services.json` actualizado (si se habilitó Storage por primera vez)
