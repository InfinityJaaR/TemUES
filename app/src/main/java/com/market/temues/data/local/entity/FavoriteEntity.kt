package com.market.temues.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "productId"],
    indices = [Index("userId"), Index("productId")]
)
data class FavoriteEntity(
    val userId: String,
    val productId: String,
    val productName: String = "",
    val productPrice: Double = 0.0,
    val productImage: String = "",
    val isSynced: Boolean = false, // control offline -> online sync
    val createdAt: Long = System.currentTimeMillis()
)