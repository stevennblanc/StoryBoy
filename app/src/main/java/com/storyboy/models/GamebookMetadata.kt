package com.storyboy.models

data class GamebookMetadata(
    val id: String,
    val title: String,
    val author: String,
    val genre: String,
    val version: String,
    val description: String,
    val startNodeId: String,
)
