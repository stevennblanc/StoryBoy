package com.storyboy.models

data class LocalGamebook(
    val metadata: GamebookMetadata,
    val filePath: String,
    val hasPlaythroughInProgress: Boolean,
)
