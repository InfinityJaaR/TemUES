package com.market.temues.data.repository

import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                trySend(
                    User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                    )
                )
            } else {
                trySend(null)
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    fun isUserAuthenticated(): Boolean = firebaseAuth.currentUser != null

    fun loginWithEmail(email: String, password: String): Flow<Result<User>> = callbackFlow {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = firebaseAuth.currentUser
                    val user = User(
                        id = firebaseUser?.uid ?: "",
                        email = firebaseUser?.email ?: email,
                        name = firebaseUser?.displayName ?: "",
                        photoUrl = firebaseUser?.photoUrl?.toString() ?: ""
                    )
                    trySend(Result.success(user))
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
                    firebaseUser?.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(name).build()
                    )
                    val user = User(
                        id = firebaseUser?.uid ?: "",
                        email = email,
                        name = name,
                        photoUrl = ""
                    )
                    firestore.collection("users").document(user.id).set(user)
                    trySend(Result.success(user))
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
                    val user = User(
                        id = firebaseUser?.uid ?: "",
                        email = firebaseUser?.email ?: "",
                        name = firebaseUser?.displayName ?: "",
                        photoUrl = firebaseUser?.photoUrl?.toString() ?: ""
                    )
                    firestore.collection("users").document(user.id).set(user)
                    trySend(Result.success(user))
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
                    val user = User(
                        id = firebaseUser?.uid ?: "",
                        email = firebaseUser?.email ?: "",
                        name = firebaseUser?.displayName ?: "",
                        photoUrl = firebaseUser?.photoUrl?.toString() ?: ""
                    )
                    firestore.collection("users").document(user.id).set(user)
                    trySend(Result.success(user))
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
}
