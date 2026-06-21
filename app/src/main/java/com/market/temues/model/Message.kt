package com.market.temues.model

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
    val isRead: Boolean = false
)
