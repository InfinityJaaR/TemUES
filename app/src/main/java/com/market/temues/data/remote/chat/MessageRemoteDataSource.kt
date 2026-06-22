package com.market.temues.data.remote.chat

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.market.temues.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private fun collection(chatId: String) =
        firestore.collection("chats").document(chatId).collection("messages")

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val registration = collection(chatId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(Message::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    suspend fun send(chatId: String, message: Message): String {
        val docRef = collection(chatId).document()
        docRef.set(message.copy(id = docRef.id)).await()
        return docRef.id
    }

    suspend fun markAsRead(chatId: String, messageId: String) {
        collection(chatId).document(messageId)
            .update("isRead", true).await()
    }

    suspend fun subirAudio(chatId: String, archivo: File): String {
        val referencia = storage.reference
            .child("chats/$chatId/audio/${UUID.randomUUID()}.m4a")
        referencia.putFile(Uri.fromFile(archivo)).await()
        return referencia.downloadUrl.await().toString()
    }
}
