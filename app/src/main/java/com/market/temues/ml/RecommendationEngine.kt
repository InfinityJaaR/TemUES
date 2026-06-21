package com.market.temues.ml

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.market.temues.model.Product
import kotlinx.coroutines.tasks.await
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationEngine @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val context: Context
) {

    private var interpreter: Interpreter? = null

    init {
        try {
            interpreter = Interpreter(loadModelFile())
        } catch (_: Exception) {
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("recommendation_model.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    suspend fun rankProducts(
        products: List<Product>,
        uid: String
    ): List<Product> {
        if (products.isEmpty()) return products

        val searchHistory = fetchSearchHistory(uid)
        if (searchHistory.isEmpty()) return products

        if (interpreter != null) {
            return rankProductsWithModel(products, searchHistory)
        }

        return rankProductsStatistical(products, searchHistory)
    }

    private suspend fun fetchSearchHistory(uid: String): List<String> {
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("searchHistory")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.getString("query")
        }
    }

    private fun rankProductsWithModel(
        products: List<Product>,
        searchHistory: List<String>
    ): List<Product> {
        val keywords = searchHistory
            .flatMap { it.lowercase().split(" ") }
            .filter { it.length > 2 }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }

        if (keywords.isEmpty()) return products

        val maxPrice = products.maxOf { it.price }.coerceAtLeast(1.0)

        return products.map { product ->
            val inputVector = buildFeatureVector(product, keywords, maxPrice)
            val output = Array(1) { FloatArray(1) }
            interpreter?.run(inputVector, output)
            Pair(product, output[0][0])
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun buildFeatureVector(
        product: Product,
        keywords: List<String>,
        maxPrice: Double
    ): Array<Array<FloatArray>> {
        val vector = FloatArray(50)

        val categories = listOf(
            "electronica", "ropa", "hogar", "deportes", "vehiculos", "servicios", "otros"
        )
        val catIndex = categories.indexOf(product.categoryId)
        if (catIndex >= 0) vector[catIndex] = 1f

        val commonTags = listOf(
            "apple", "samsung", "nike", "gamer", "usado", "nuevo", "laptop", "celular", "zapatos", "tv"
        )
        for ((i, tag) in commonTags.withIndex()) {
            if (product.tags.any { it.lowercase() == tag }) {
                vector[7 + i] = 1f
            }
        }

        val productText = (product.name + " " + product.categoryName + " " + product.tags.joinToString(" ")).lowercase()
        for ((i, keyword) in keywords.withIndex()) {
            if (productText.contains(keyword)) {
                vector[17 + i] = 1f
            }
        }

        vector[27] = (product.price / maxPrice).toFloat().coerceIn(0f, 1f)

        vector[37] = if (product.condition == "nuevo") 1f else 0f

        return arrayOf(arrayOf(vector))
    }

    private fun rankProductsStatistical(
        products: List<Product>,
        searchHistory: List<String>
    ): List<Product> {
        val keywords = searchHistory
            .flatMap { it.lowercase().split(" ") }
            .filter { it.length > 2 }
            .groupingBy { it }
            .eachCount()

        if (keywords.isEmpty()) return products

        val maxKeywordFreq = keywords.values.max().toFloat()

        return products.map { product ->
            val score = calculateScore(product, keywords, maxKeywordFreq)
            Pair(product, score)
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun calculateScore(
        product: Product,
        keywords: Map<String, Int>,
        maxFreq: Float
    ): Float {
        var score = 0f

        val nameWords = product.name.lowercase().split(" ")
        for ((keyword, freq) in keywords) {
            if (nameWords.any { it.contains(keyword) }) {
                score += 3f * (freq / maxFreq)
            }
        }

        val categoryLower = product.categoryName.lowercase()
        for ((keyword, freq) in keywords) {
            if (categoryLower.contains(keyword)) {
                score += 2f * (freq / maxFreq)
            }
        }

        for (tag in product.tags) {
            val tagLower = tag.lowercase()
            for ((keyword, freq) in keywords) {
                if (tagLower.contains(keyword)) {
                    score += 2f * (freq / maxFreq)
                }
            }
        }

        val descLower = product.description.lowercase()
        for ((keyword, freq) in keywords) {
            if (descLower.contains(keyword)) {
                score += 1f * (freq / maxFreq)
            }
        }

        return score
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
