package com.market.temues.model

data class Category(
    val id: String = "",
    val name: String = "",
    val iconUrl: String = "",
    val parentId: String? = null,
    val order: Int = 0
)
