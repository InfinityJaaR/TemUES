package com.market.temues.data.remote.orden

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.market.temues.model.Orden
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdenRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val coleccion = firestore.collection("orders")

    suspend fun crear(orden: Orden): String {
        val ref = coleccion.document()
        val ordenConId = orden.copy(id = ref.id)
        ref.set(ordenConId).await()
        return ref.id
    }

    fun obtenerOrdenesUsuario(usuarioId: String): Flow<List<Orden>> = callbackFlow {
        val listener = coleccion
            .whereEqualTo("usuarioId", usuarioId)
            .orderBy("creadoEn", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val ordenes = snap?.documents?.mapNotNull {
                    it.toObject(Orden::class.java)
                } ?: emptyList()
                trySend(ordenes)
            }
        awaitClose { listener.remove() }
    }

    fun obtenerOrdenesVendedor(vendedorId: String): Flow<List<Orden>> = callbackFlow {
        val listener = coleccion
            .whereEqualTo("vendedorId", vendedorId)
            .orderBy("creadoEn", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val ordenes = snap?.documents?.mapNotNull {
                    it.toObject(Orden::class.java)
                } ?: emptyList()
                trySend(ordenes)
            }
        awaitClose { listener.remove() }
    }

    suspend fun actualizarEstado(ordenId: String, estado: String) {
        coleccion.document(ordenId).update("estado", estado).await()
    }
}
