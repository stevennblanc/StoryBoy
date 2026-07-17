package com.storyboy.models

data class StoreGamebook(
    val metadata: GamebookMetadata,
    val downloadUrl: String,
    val posterPath: String?,
    val bannerPath: String?,
    val isDownloaded: Boolean,
    val localVersion: String?,
    val updateAvailable: Boolean,
)
