package com.market.temues.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "ahora"
            diff < 3_600_000 -> "${diff / 60_000} min"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            diff < 604_800_000 -> "${diff / 86_400_000}d"
            else -> formatTimestamp(timestamp)
        }
    }
}
