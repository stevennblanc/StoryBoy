package com.storyboy.engine

data class InventoryItem(
    val id: String,
    val title: String,
    val description: String,
    val detail: String = "",
    val image: String? = null,
)
