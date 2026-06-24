package com.market.temues.model

import com.google.firebase.firestore.PropertyName

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val type: String = "text",
    val duracionSegundos: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false
)
