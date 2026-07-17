package com.storyboy.models

data class StoreGamebook(
    val metadata: GamebookMetadata,
    val downloadUrl: String,
    val isDownloaded: Boolean,
)
