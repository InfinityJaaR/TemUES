package com.market.temues.data.remote.chat

import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
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
}
