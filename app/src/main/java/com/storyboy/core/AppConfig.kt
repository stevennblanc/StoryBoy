package com.storyboy.core

import com.storyboy.BuildConfig

object AppConfig {
    const val AppName = "StoryBoy"
    const val GamebookExtension = "gbk"
    const val GamebooksDirectory = "gamebooks"
    const val ProgressPreferences = "storyboy_progress"
    const val UpdateApkFileName = "storyboy-update.apk"

    val UpdateManifestUrl: String = BuildConfig.UPDATE_MANIFEST_URL
    val StoreIndexUrl: String = BuildConfig.STORE_INDEX_URL
}
