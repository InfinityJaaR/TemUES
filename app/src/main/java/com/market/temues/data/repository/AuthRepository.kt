package com.market.temues.data.repository

import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentUser: Flow<User?> = callbackFlow {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val listener = FirebaseAuth.AuthStateListener { auth ->
            scope.launch {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val user = fetchUserFromFirestore(firebaseUser.uid)
                    trySend(user)
                } else {
                    trySend(null)
                }
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            scope.cancel()
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    fun isUserAuthenticated(): Boolean = firebaseAuth.currentUser != null

    fun loginWithEmail(email: String, password: String): Flow<Result<User>> = callbackFlow {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = firebaseAuth.currentUser
                    if (firebaseUser != null) {
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            try {
                                val user = fetchUserFromFirestore(firebaseUser.uid)
                                trySend(Result.success(user))
                            } catch (e: Exception) {
                                trySend(Result.failure(e))
                            }
                        }
                    } else {
                        trySend(Result.failure(Exception("Error al iniciar sesión")))
                    }
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Error al iniciar sesión")))
                }
            }
        awaitClose { }
    }

    fun registerWithEmail(name: String, email: String, password: String): Flow<Result<User>> = callbackFlow {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = firebaseAuth.currentUser
                    if (firebaseUser != null) {
                        firebaseUser.updateProfile(
                            UserProfileChangeRequest.Builder().setDisplayName(name).build()
                        )
                        val user = User(
                            id = firebaseUser.uid,
                            email = email,
                            name = name,
                            photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                        )
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            try {
                                firestore.collection("users").document(user.id).set(user).await()
                                val created = fetchUserFromFirestore(firebaseUser.uid)
                                trySend(Result.success(created))
                            } catch (e: Exception) {
                                trySend(Result.success(user))
                            }
                        }
                    } else {
                        trySend(Result.failure(Exception("Error al registrarse")))
                    }
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Error al registrarse")))
                }
            }
        awaitClose { }
    }

    fun signInWithGoogle(idToken: String): Flow<Result<User>> = callbackFlow {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = firebaseAuth.currentUser
                    if (firebaseUser != null) {
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            try {
                                val user = fetchUserFromFirestore(firebaseUser.uid)
                                firestore.collection("users").document(user.id).set(user).await()
                                trySend(Result.success(user))
                            } catch (e: Exception) {
                                trySend(Result.success(
                                    User(
                                        id = firebaseUser.uid,
                                        email = firebaseUser.email ?: "",
                                        name = firebaseUser.displayName ?: ""
                                    )
                                ))
                            }
                        }
                    } else {
                        trySend(Result.failure(Exception("Error con Google Sign-In")))
                    }
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Error con Google Sign-In")))
                }
            }
        awaitClose { }
    }

    fun signInWithFacebook(accessToken: String): Flow<Result<User>> = callbackFlow {
        val credential = FacebookAuthProvider.getCredential(accessToken)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = firebaseAuth.currentUser
                    if (firebaseUser != null) {
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            try {
                                val user = fetchUserFromFirestore(firebaseUser.uid)
                                firestore.collection("users").document(user.id).set(user).await()
                                trySend(Result.success(user))
                            } catch (e: Exception) {
                                trySend(Result.success(
                                    User(
                                        id = firebaseUser.uid,
                                        email = firebaseUser.email ?: "",
                                        name = firebaseUser.displayName ?: ""
                                    )
                                ))
                            }
                        }
                    } else {
                        trySend(Result.failure(Exception("Error con Facebook Login")))
                    }
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Error con Facebook Login")))
                }
            }
        awaitClose { }
    }

    fun sendPasswordReset(email: String): Flow<Result<Unit>> = callbackFlow {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(Result.success(Unit))
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Error al enviar correo")))
                }
            }
        awaitClose { }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    private suspend fun fetchUserFromFirestore(uid: String): User {
        val doc = firestore.collection("users").document(uid).get().await()
        return if (doc.exists()) {
            userFromSnapshot(doc, uid)
        } else {
            val fbUser = firebaseAuth.currentUser
            User(
                id = uid,
                email = fbUser?.email ?: "",
                name = fbUser?.displayName ?: ""
            )
        }
    }

    private fun userFromSnapshot(doc: DocumentSnapshot, uid: String): User {
        val data = doc.data ?: return User(id = uid)
        return User(
            id = uid,
            email = data["email"] as? String ?: "",
            name = data["name"] as? String ?: "",
            photoUrl = data["photoUrl"] as? String ?: "",
            isAdmin = data["isAdmin"] as? Boolean ?: false,
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
        )
    }
}
