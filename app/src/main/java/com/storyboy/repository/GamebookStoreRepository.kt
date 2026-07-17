package com.storyboy.repository

import android.content.Context
import com.storyboy.core.AppConfig
import com.storyboy.data.GamebookStorage
import com.storyboy.data.StoreIndexParser
import com.storyboy.models.GamebookMetadata
import com.storyboy.models.StoreGamebook
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class GamebookStoreRepository(context: Context) {
    private val appContext = context.applicationContext
    private val storage = GamebookStorage(context.applicationContext)
    private val storeArtworkDir: File
        get() = File(appContext.filesDir, "store_art").apply { mkdirs() }

    fun listStoreGamebooks(): List<StoreGamebook> {
        val localBooks = storage.listGamebookFiles()
            .mapNotNull { file ->
                runCatching {
                    val metadata = storage.readMetadata(file)
                    val artwork = storage.extractArtwork(file, metadata)
                    LocalStoreBook(
                        metadata = metadata,
                        posterPath = artwork.posterPath,
                        bannerPath = artwork.bannerPath,
                    )
                }.getOrNull()
            }
        val localVersions = localBooks.associate { it.metadata.id to it.metadata.version }
        val localPosterPaths = localBooks.associate { it.metadata.id to it.posterPath }
        val localBannerPaths = localBooks.associate { it.metadata.id to it.bannerPath }
        val json = getText(AppConfig.StoreIndexUrl)
        val source = JSONObject(json)
        val remotePosterPaths = cacheRemoteArtwork(source, "posterUrl", "coverImageUrl", "poster_url", "cover_image_url")
        val remoteBannerPaths = cacheRemoteArtwork(source, "bannerUrl", "bannerImageUrl", "banner_url", "banner_image_url")
        return StoreIndexParser.parse(
            json = json,
            localVersions = localVersions,
            localPosterPaths = localPosterPaths,
            localBannerPaths = localBannerPaths,
            remotePosterPaths = remotePosterPaths,
            remoteBannerPaths = remoteBannerPaths,
        )
    }

    fun download(storeGamebook: StoreGamebook) {
        storage.downloadGamebook(storeGamebook.downloadUrl)
    }

    private fun getText(url: String): String {
        return URL(url).openConnection().asHttpConnection().use { connection ->
            connection.inputStream.bufferedReader().use { it.readText() }
        }
    }

    private fun cacheRemoteArtwork(source: JSONObject, vararg fieldNames: String): Map<String, String?> {
        val entries = source.optJSONArray("gamebooks") ?: return emptyMap()
        return buildMap {
            for (index in 0 until entries.length()) {
                val item = entries.getJSONObject(index)
                val id = item.optString("id")
                val imageUrl = fieldNames.firstNotNullOfOrNull { field ->
                    item.optString(field).takeIf { it.isNotBlank() }
                }
                if (id.isNotBlank() && imageUrl != null) {
                    put(id, downloadArtwork(id, imageUrl))
                }
            }
        }
    }

    private fun downloadArtwork(gamebookId: String, url: String): String? {
        return runCatching {
            val extension = url.substringBefore('?').substringAfterLast('.', "png").ifBlank { "png" }
            val targetFile = File(storeArtworkDir, "$gamebookId-${url.hashCode()}.$extension")
            if (!targetFile.exists()) {
                URL(url).openConnection().asImageConnection().use { connection ->
                    connection.inputStream.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            targetFile.absolutePath
        }.getOrNull()
    }

    private data class LocalStoreBook(
        val metadata: GamebookMetadata,
        val posterPath: String?,
        val bannerPath: String?,
    )
}

private fun java.net.URLConnection.asHttpConnection(): HttpURLConnection {
    return (this as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        requestMethod = "GET"
        instanceFollowRedirects = true
        setRequestProperty("Accept", "application/json")
    }
}

private fun java.net.URLConnection.asImageConnection(): HttpURLConnection {
    return (this as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        requestMethod = "GET"
        instanceFollowRedirects = true
        setRequestProperty("Accept", "image/*")
    }
}

private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T {
    return try {
        if (responseCode !in 200..299) {
            error("Store request failed with HTTP $responseCode")
        }
        block(this)
    } finally {
        disconnect()
    }
}
