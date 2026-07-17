package com.storyboy.data

import com.storyboy.models.GamebookMetadata
import com.storyboy.models.StoreGamebook
import org.json.JSONObject

object StoreIndexParser {
    fun parse(
        json: String,
        localVersions: Map<String, String>,
        localPosterPaths: Map<String, String?> = emptyMap(),
        localBannerPaths: Map<String, String?> = emptyMap(),
        remotePosterPaths: Map<String, String?> = emptyMap(),
        remoteBannerPaths: Map<String, String?> = emptyMap(),
    ): List<StoreGamebook> {
        val source = JSONObject(json)
        val entries = source.optJSONArray("gamebooks") ?: return emptyList()

        return buildList {
            for (index in 0 until entries.length()) {
                val item = entries.getJSONObject(index)
                val metadata = GamebookMetadata(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    author = item.optString("author", "Unknown author"),
                    genre = item.optString("genre", "Gamebook"),
                    version = item.optString("version", "1.0.0"),
                    description = item.optString("description"),
                    startNodeId = item.optString("startNodeId"),
                )
                add(
                    StoreGamebook(
                        metadata = metadata,
                        downloadUrl = item.getString("downloadUrl"),
                        posterPath = localPosterPaths[metadata.id] ?: remotePosterPaths[metadata.id],
                        bannerPath = localBannerPaths[metadata.id] ?: remoteBannerPaths[metadata.id],
                        isDownloaded = metadata.id in localVersions,
                        localVersion = localVersions[metadata.id],
                        updateAvailable = localVersions[metadata.id]?.let { it != metadata.version } ?: false,
                    ),
                )
            }
        }
    }
}
