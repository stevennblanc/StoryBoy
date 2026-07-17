package com.storyboy.models

data class LocalGamebook(
    val metadata: GamebookMetadata,
    val filePath: String,
    val posterPath: String?,
    val bannerPath: String?,
    val hasPlaythroughInProgress: Boolean,
)
