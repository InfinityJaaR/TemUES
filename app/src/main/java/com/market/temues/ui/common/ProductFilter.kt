package com.market.temues.ui.common

import com.market.temues.model.Product

fun Product.matchesSearch(text: String): Boolean {
    val productText = listOf(
        name,
        description,
        price.toString(),
        categoryId,
        categoryName,
        sellerName,
        condition,
        location,
        tags.joinToString(" ")
    ).joinToString(" ").lowercase()
    return productText.contains(text)
}
