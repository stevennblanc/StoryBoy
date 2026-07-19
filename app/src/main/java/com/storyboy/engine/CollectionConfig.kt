package com.storyboy.engine

data class CollectionConfig(
    val label: String,
    val showCount: Boolean = true,
    val enabled: Boolean = false,
) {
    companion object {
        const val DefaultInventoryLabel = "Items"
        const val DefaultEvidenceLabel = "Evidence"
    }
}
