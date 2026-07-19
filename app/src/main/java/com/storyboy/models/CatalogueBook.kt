package com.storyboy.models

data class CatalogueBook(
    val id: String,
    val title: String,
    val author: String,
    val genre: String,
    val description: String,
    val about: String,
    val version: String,
    val priceUsd: Double,
    val language: String,
    val publisher: String,
    val publishedOn: String,
    val nodeCount: Int?,
    val endingCount: Int?,
    val fileSizeBytes: Long?,
    val features: List<String>,
) {
    val isFree: Boolean get() = priceUsd <= 0.0

    fun priceLabel(): String {
        return if (isFree) "Free" else "$" + String.format("%.2f", priceUsd)
    }

    fun fileSizeLabel(): String? {
        val bytes = fileSizeBytes ?: return null
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
