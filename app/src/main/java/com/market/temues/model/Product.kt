package com.market.temues.model

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val categoryId: String = "",
    val categoryName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val images: List<String> = emptyList(),
    val condition: String = "usado",
    val location: String = "",
    val tags: List<String> = emptyList(),
    val status: String = "activo",
    val hasStock: Boolean = false,
    val stock: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
