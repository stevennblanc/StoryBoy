package com.storyboy.core

import com.storyboy.BuildConfig

object AppConfig {
    const val AppName = "StoryBoy"
    const val UpdateApkFileName = "storyboy-update.apk"

    val UpdateManifestUrl: String = BuildConfig.UPDATE_MANIFEST_URL
}
