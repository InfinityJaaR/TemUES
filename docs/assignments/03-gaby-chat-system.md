# Gaby — Sistema de Chat y Notificaciones Push

## Objetivo

Implementar el sistema de mensajería completo: lista de chats del usuario, conversación en tiempo real, envío de mensajes de texto y audio, integración con llamada telefónica, **y notificaciones push (FCM) para mensajes entrantes**.

## Estado actual (ya existe)

- `ChatListFragment.kt` — stub vacío (solo infla `fragment_chat_list.xml`)
- `ChatRemoteDataSource` — `getUserChats(userId)`, `getById(chatId)`, `createOrGet(p1, p2, productId)`, `updateLastMessage(chatId, text, senderId)`
- `MessageRemoteDataSource` — `getMessages(chatId)`, `send(chatId, message)`, `markAsRead(chatId, messageId)`
- `StorageDataSource` — `uploadProductImage(imageUri)` (reutilizable para audio)
- Modelos `Chat` y `Message` completos
- `UserRemoteDataSource` — `getById(userId)` para obtener nombre/foto del otro participante
- `fragment_chat_list.xml` — layout placeholder
- `nav_graph.xml` — ya tiene destino `chatFragment` (ChatListFragment)
- `firebase.messaging` declarado en dependencias
- Permiso `RECORD_AUDIO` en AndroidManifest (verificar)

## Tareas

### 1. FirebaseMessagingService

- **Ubicación**: `app/src/main/java/com/market/temues/service/TemUESMessagingService.kt`
- Extender `FirebaseMessagingService()`
- Registrar en `AndroidManifest.xml`:

```xml
<service
    android:name=".service.TemUESMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

- `onNewToken(token: String)`:
  - Guardar token en Firestore en `users/{uid}/fcmToken`
  - O en SharedPreferences para usarlo después
- `onMessageReceived(remoteMessage: RemoteMessage)`:
  - Extraer datos del mensaje (`type`, `chatId`, `senderName`, `text`)
  - Si `type == "chat_message"`:
    - Crear notificación con `NotificationCompat.Builder`
    - Canal de notificación: `chat_messages`
    - Título: `senderName`, texto: `text`
    - Al hacer clic en la notificación → abrir `chatDetailFragment` con `chatId`
    - Si la app está en primer plano y en el chat correspondiente, no mostrar notificación
- `handleNotificationTap(intent: Intent)`:
  - Extraer `chatId` del intent extra
  - Navegar a `chatDetailFragment` con ese `chatId`

### 2. Notificación Channel

- **Ubicación**: `app/src/main/java/com/market/temues/TemUESApp.kt`
- Crear canal de notificación en `onCreate()`:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val channel = NotificationChannel(
        "chat_messages",
        "Mensajes de chat",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Notificaciones de nuevos mensajes en el chat"
    }
    val notificationManager = getSystemService(NotificationManager::class.java)
    notificationManager.createNotificationChannel(channel)
}
```

### 3. Enviar FCM al enviar mensaje

En `ChatDetailViewModel.sendMessage()` y `sendAudio()`, después de enviar el mensaje a Firestore:
- Construir datos para notificación:
  ```kotlin
  val notificationData = mapOf(
      "type" to "chat_message",
      "chatId" to chatId,
      "senderId" to currentUserId,
      "senderName" to currentUserName,
      "text" to text
  )
  ```
- Opción A (simplificada): Crear un documento en `chats/{chatId}/notifications/{id}` con los datos
- Opción B (más directa pero requiere backend): Usar Firebase Functions para enviar FCM
- **Para MVP**: Usar la Opción A y hacer que `TemUESMessagingService` escuche cambios en la colección de notificaciones del usuario (`users/{uid}/notifications/`) además de los mensajes FCM directos

### 4. ChatListViewModel (nuevo)

- **Ubicación**: `app/src/main/java/com/market/temues/ui/chat/ChatListViewModel.kt`
- Inyectar `ChatRemoteDataSource`, `UserRemoteDataSource`, `FirebaseAuth`
- Obtener `currentUser.uid`
- `val chats: StateFlow<List<Chat>>` — de `chatRemoteDataSource.getUserChats(uid)`
- `sealed class ChatListUiState { Loading, Success(chats), Error(message) }`
- `val uiState: StateFlow<ChatListUiState>`
- `fun openChat(chatId: String)` — emitir evento de navegación

### 5. ChatListFragment (mejorar el stub existente)

- Inyectar `ChatListViewModel` con `by viewModels()`
- `RecyclerView` con `LinearLayoutManager`
- Adapter: `ChatListAdapter` con ViewBinding para `item_chat_list.xml`
- Cada item muestra:
  - Avatar del otro usuario (circular, Glide, o iniciales si no hay foto)
  - Nombre del otro participante (consultar de `UserRemoteDataSource`)
  - Último mensaje (truncado a 2 líneas)
  - Timestamp relativo ("hace 2 min", "ayer", "12/05")
  - Indicador de mensaje no leído (badge)
- Estados loading, empty ("No tienes conversaciones"), error
- Al hacer clic en un chat → navegar a `chatDetailFragment` con `chatId`
- SwipeRefreshLayout opcional

### 6. ChatParticipantHelper (opcional)

- **Ubicación**: `app/src/main/java/com/market/temues/ui/chat/ChatParticipantHelper.kt`
- `suspend fun getOtherParticipantName(chat: Chat, currentUserId: String): String`
- Buscar el ID del otro usuario en `chat.participants`, llamar `userRemoteDataSource.getById()`, devolver `name`
- Cachear resultados en `Map<String, String>` en memoria

### 7. ChatDetailViewModel (nuevo)

- **Ubicación**: `app/src/main/java/com/market/temues/ui/chat/ChatDetailViewModel.kt`
- Inyectar `MessageRemoteDataSource`, `ChatRemoteDataSource`, `FirebaseAuth`, `StorageDataSource`, `FirebaseFirestore`
- Tomar `chatId` como argumento (SavedStateHandle)
- `val messages: StateFlow<List<Message>>` — de `messageRemoteDataSource.getMessages(chatId)`
- `val chat: StateFlow<Chat?>` — de `chatRemoteDataSource.getById(chatId)`
- `fun sendMessage(text: String)`:
  - Crear `Message(senderId = currentUid, chatId = chatId, text = text)`
  - Llamar `messageRemoteDataSource.send(chatId, message)`
  - Llamar `chatRemoteDataSource.updateLastMessage(chatId, text, currentUid)`
  - **Disparar notificación**: escribir en `users/{otherUserId}/notifications/` con datos del mensaje, o enviar FCM directo
- `fun sendAudio(audioUri: Uri)`:
  - Subir a Storage usando `StorageDataSource.uploadProductImage(audioUri)`
  - Una vez obtenida la URL, crear `Message` con `imageUrl = audioUrl`
  - Enviar igual que mensaje de texto + disparar notificación
- `fun markAsRead(messageId: String)` — llamar `messageRemoteDataSource.markAsRead()`
- `val currentUserId: String`
- `sealed class ChatDetailUiState { Loading, Success(chat, messages), Error }`

### 8. ChatDetailFragment (nuevo)

- **Ubicación**: `app/src/main/java/com/market/temues/ui/chat/ChatDetailFragment.kt`
- **Layout**: `res/layout/fragment_chat_detail.xml`
- Toolbar: nombre del otro usuario, avatar, botón de llamada
- `RecyclerView` de mensajes con `LinearLayoutManager` desde abajo (`stackFromEnd = true`)
- Adapter con múltiples view types:
  - Mensaje enviado (derecha, burbuja azul/verde)
  - Mensaje recibido (izquierda, burbuja gris)
  - Mensaje de audio enviado (derecha, con ícono play + duración)
  - Mensaje de audio recibido (izquierda, con ícono play + duración)
- Input field en la parte inferior:
  - `EditText` multilinea (max 3 líneas)
  - Botón micrófono (para grabar audio)
  - Botón enviar (flecha o ícono)
- Scroll automático al último mensaje cuando llega uno nuevo
- Marcar mensajes como leídos al visualizarlos

### 9. Audio Recording

#### Grabar audio

- Usar `MediaRecorder` con `AudioSource.MIC`
- Output en formato `MediaRecorder.OutputFormat.MPEG_4` (`.mp4`) o `THREE_GPP` (`.3gp`)
- Al presionar micrófono: comenzar a grabar, cambiar ícono a "detener"
- Al soltar/detener: guardar archivo en `context.cacheDir`
- Mostrar preview del audio grabado con opción de escuchar antes de enviar
- Al confirmar: llamar `sendAudio(audioUri)` del ViewModel

#### Reproducir audio

- En cada burbuja de audio: botón play/pause
- Usar `MediaPlayer` para reproducir
- `setOnCompletionListener` para resetear el ícono
- Solo un audio reproduce a la vez (detener el anterior al iniciar otro)
- Mostrar duración del audio (formato "0:32")

#### Permisos

- Solicitar `RECORD_AUDIO` en runtime (API 23+)
- Usar `registerForActivityResult(ActivityResultContracts.RequestPermission())`
- Si no se concede, mostrar Snackbar explicativo

### 10. Phone Call

- Botón de llamada en el toolbar del ChatDetailFragment
- Al presionar: obtener número del otro usuario (de `User.phone` o mostrar diálogo para ingresarlo)
- Abrir marcador: `Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phoneNumber") }`

### 11. Iniciar chat desde ProductDetail

- Cuando el usuario presiona 💬 en ProductDetailFragment:
  - Obtener `sellerId` y `productId` desde el producto
  - Llamar `ChatRemoteDataSource.createOrGet(currentUid, sellerId, productId)`
  - Navegar a `chatDetailFragment` con el `chatId` resultante
- Esto debe coordinarse con Yami (ProductDetailFragment llama la navegación)

### 12. Layouts

| Layout | Archivo | Descripción |
|---|---|---|
| Chat list item | `res/layout/item_chat_list.xml` | Avatar circular, nombre, último mensaje, timestamp, badge no leído |
| Message sent | `res/layout/item_chat_message_sent.xml` | Burbuja alineada a la derecha, color primario |
| Message received | `res/layout/item_chat_message_received.xml` | Burbuja alineada a la izquierda, color gris |
| Audio sent | `res/layout/item_chat_audio_sent.xml` | Burbuja con ícono play, barra de progreso, duración |
| Audio received | `res/layout/item_chat_audio_received.xml` | Burbuja con ícono play, barra de progreso, duración |

### 13. Navegación (añadir a nav_graph.xml)

```xml
<fragment
    android:id="@+id/chatDetailFragment"
    android:name="com.market.temues.ui.chat.ChatDetailFragment"
    android:label="@string/chat_detail_title">
    <argument
        android:name="chatId"
        android:defaultValue="" />
    <argument
        android:name="otherUserId"
        android:defaultValue="" />
    <argument
        android:name="productId"
        android:defaultValue="" />
</fragment>
```

Acciones a agregar:
- `chatFragment` → `chatDetailFragment`
- `productDetailFragment` → `chatDetailFragment`

### 14. Strings

```xml
<string name="chat_detail_title">Chat</string>
<string name="chat_list_empty">No tienes conversaciones</string>
<string name="chat_input_hint">Escribe un mensaje...</string>
<string name="chat_you_label">Tú</string>
<string name="chat_send">Enviar</string>
<string name="chat_record_audio">Grabar audio</string>
<string name="chat_permission_audio">Se necesita permiso para grabar audio</string>
```

## Criterios de aceptación

- [ ] Lista de chats carga en tiempo real y muestra último mensaje con timestamp
- [ ] Al abrir un chat se ven los mensajes ordenados cronológicamente
- [ ] Se pueden enviar y recibir mensajes de texto instantáneamente
- [ ] Se puede grabar y enviar audio
- [ ] Los audios se reproducen al hacer clic con indicador de estado
- [ ] Botón de llamada abre el marcador con el número del usuario
- [ ] Al hacer clic en 💬 desde ProductDetail se abre un chat nuevo o existente
- [ ] Los mensajes se marcan como leídos al visualizarse
- [ ] **Llegan notificaciones push cuando la app está en segundo plano y alguien envía un mensaje**
- [ ] **Al tocar la notificación, se abre el chat correspondiente**

## Dependencias

- FCM (`firebase.messaging`) ya declarado en dependencias
- `MediaRecorder` y `MediaPlayer` son APIs estándar de Android SDK
- `UserRemoteDataSource` existe para obtener nombre/número del otro usuario
