package com.market.temues.model

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
