package com.market.temues.model

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val unreadCounts: Map<String, Long> = emptyMap()
)
