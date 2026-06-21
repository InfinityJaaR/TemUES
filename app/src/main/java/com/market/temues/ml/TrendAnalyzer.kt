package com.market.temues.ml

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrendAnalyzer @Inject constructor() {

    data class TrendResult(
        val categoryName: String,
        val productCount: Int,
        val trend: String
    )

    fun analyze(categoryCounts: Map<String, Int>): List<TrendResult> {
        if (categoryCounts.isEmpty()) return emptyList()
        val avgCount = categoryCounts.values.average()
        return categoryCounts.map { (name, count) ->
            val trend = when {
                count > avgCount * 1.2 -> "up"
                count < avgCount * 0.8 -> "down"
                else -> "stable"
            }
            TrendResult(name, count, trend)
        }.sortedByDescending { it.productCount }
    }
}
