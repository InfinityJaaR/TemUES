package com.market.temues.data.remote.chat

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.model.Chat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("chats")

    fun getUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val registration = collection
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.documents?.mapNotNull {
                    it.toObject(Chat::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(chats)
            }
        awaitClose { registration.remove() }
    }

    fun getById(chatId: String): Flow<Chat?> = callbackFlow {
        val registration = collection.document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chat = snapshot?.toObject(Chat::class.java)?.copy(id = snapshot.id)
                trySend(chat)
            }
        awaitClose { registration.remove() }
    }

    suspend fun createOrGet(participant1: String, participant2: String, productId: String = ""): String {
        val existing = collection
            .whereArrayContains("participants", participant1)
            .get().await()
            .documents
            .firstOrNull { doc ->
                val chat = doc.toObject(Chat::class.java)
                chat != null && chat.participants.contains(participant2)
            }

        if (existing != null) return existing.id

        val newChat = Chat(
            participants = listOf(participant1, participant2),
            productId = productId,
            createdAt = System.currentTimeMillis()
        )
        val docRef = collection.document()
        docRef.set(newChat.copy(id = docRef.id)).await()
        return docRef.id
    }

    suspend fun updateLastMessage(chatId: String, text: String, senderId: String) {
        collection.document(chatId).update(
            mapOf(
                "lastMessage" to text,
                "lastMessageSenderId" to senderId,
                "lastMessageTimestamp" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun incrementarNoLeidos(chatId: String, recipientId: String) {
        collection.document(chatId)
            .update("unreadCounts.$recipientId", FieldValue.increment(1))
            .await()
    }

    suspend fun resetearNoLeidos(chatId: String, uid: String) {
        if (uid.isBlank()) return
        collection.document(chatId)
            .update("unreadCounts.$uid", 0)
            .await()
    }
}
