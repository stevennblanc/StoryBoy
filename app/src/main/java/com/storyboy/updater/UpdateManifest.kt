package com.storyboy.updater

import org.json.JSONObject

data class UpdateManifest(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
) {
    companion object {
        fun fromJson(json: String): UpdateManifest {
            val source = JSONObject(json)
            return UpdateManifest(
                versionCode = source.getLong("versionCode"),
                versionName = source.getString("versionName"),
                apkUrl = source.getString("apkUrl"),
                releaseNotes = source.optString("releaseNotes"),
            )
        }
    }
}
