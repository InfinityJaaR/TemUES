package com.market.temues.utils

object ValidationUtils {
    fun isValidPrice(price: String): Boolean =
        price.toDoubleOrNull()?.let { it > 0 } ?: false

    fun isNotEmpty(vararg fields: String?): Boolean =
        fields.all { !it.isNullOrBlank() }
}
