# Autenticación — Firebase Auth

## Providers habilitados en Firebase Console

- Email/Contraseña
- Google Sign-In
- Facebook Login

## Modelo

```kotlin
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
```

## AuthRepository

`data/repository/AuthRepository.kt` — Punto único de acceso a Firebase Auth.

### Métodos

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `currentUser` | `Flow<User?>` | Observable del usuario autenticado (escucha cambios) |
| `isUserAuthenticated()` | `Boolean` | Si hay sesión activa |
| `loginWithEmail(email, password)` | `Flow<Result<User>>` | Login con email/contraseña |
| `registerWithEmail(name, email, password)` | `Flow<Result<User>>` | Registro + guarda en Firestore |
| `signInWithGoogle(idToken)` | `Flow<Result<User>>` | Google Sign-In con token |
| `signInWithFacebook(accessToken)` | `Flow<Result<User>>` | Facebook Login con token |
| `sendPasswordReset(email)` | `Flow<Result<Unit>>` | Envía email de recuperación |
| `signOut()` | `Unit` | Cierra sesión en Firebase |

**Inyección**: Recibe `FirebaseAuth` y `FirebaseFirestore` vía Hilt desde `FirebaseModule`.

## AuthViewModel

`ui/auth/AuthViewModel.kt`

### Estados

```kotlin
sealed class AuthUiState {
    data object Idle
    data object Loading
    data class Success(val user: User)
    data object PasswordResetSent
    data class Error(val message: String)
}
```

### Flujo de Login (email/contraseña)

1. `LoginFragment` → `authViewModel.loginWithEmail(email, pass)`
2. ViewModel → `_uiState = Loading`
3. ViewModel → `authRepository.loginWithEmail()` → `FirebaseAuth.signInWithEmailAndPassword()`
4. Repository emite `Result.success(user)` o `Result.failure(error)` via `callbackFlow`
5. ViewModel → `_uiState = Success(user)` o `Error(message)`
6. `LoginFragment` observa con `.asLiveData().observe()` → navega a home o muestra Snackbar

### Flujo de Google Sign-In

1. `LoginFragment` lanza `googleSignInLauncher` (ActivityResultLauncher)
2. Usuario selecciona cuenta → Google devuelve `GoogleSignInAccount`
3. Fragment extrae `idToken` → `authViewModel.signInWithGoogle(idToken)`
4. `AuthRepository` → `GoogleAuthProvider.getCredential(idToken)` → `FirebaseAuth.signInWithCredential()`
5. Firebase crea la cuenta si no existe, o inicia sesión si ya existe

### Flujo de Facebook Login

1. `LoginFragment` → `LoginManager.logInWithReadPermissions(this, callbackManager, permissions)`
2. Facebook devuelve `LoginResult` con `AccessToken`
3. Fragment extrae token → `authViewModel.signInWithFacebook(token)`
4. `AuthRepository` → `FacebookAuthProvider.getCredential(token)` → `FirebaseAuth.signInWithCredential()`

### Flujo de Registro

1. `RegisterFragment` valida: nombre, email, password (mín. 6), confirmación
2. `AuthRepository.registerWithEmail()` → `FirebaseAuth.createUserWithEmailAndPassword()`
3. Se actualiza el perfil: `UserProfileChangeRequest.setDisplayName(name)`
4. Se guarda en Firestore: `firestore.collection("users").document(user.id).set(user)`
5. Después de registro exitoso → navega a home

### Flujo de Recuperación de Contraseña

1. `ForgotPasswordFragment` → `authViewModel.sendPasswordReset(email)`
2. `AuthRepository` → `FirebaseAuth.sendPasswordResetEmail()`
3. ViewModel emite `AuthUiState.PasswordResetSent`
4. Fragment muestra Snackbar de confirmación

### Cierre de Sesión

1. `ProfileFragment` → botón "Cerrar sesión"
2. Primero: `GoogleSignIn.getClient(activity, gso).signOut()` (borra cuenta cacheada de Google)
3. Después: `authViewModel.signOut()` → `FirebaseAuth.signOut()`
4. Navega a `loginFragment` limpiando backstack (`popUpToInclusive=true`)

## Configuración en Firebase Console

1. **Email/Password**: Authentication → Sign-in method → Habilitar
2. **Google**: Habilitar → se genera automáticamente el Web client ID
3. **SHA-1**: Project Settings → Your apps → Android → Add fingerprint
4. **google-services.json**: Re-descargar después de habilitar Google

## Configuración de Facebook

1. Registrar app en [developers.facebook.com](https://developers.facebook.com)
2. Obtener App ID
3. En `res/values/strings.xml`:

```xml
<string name="facebook_app_id">AQUI_TU_ID</string>
<string name="fb_login_protocol_scheme">fbAQUI_TU_ID</string>
```

4. En `AndroidManifest.xml` (ya está configurado):
```xml
<meta-data android:name="com.facebook.sdk.ApplicationId"
    android:value="@string/facebook_app_id" />
```
