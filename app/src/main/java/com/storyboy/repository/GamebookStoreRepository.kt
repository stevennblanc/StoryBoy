package com.storyboy.repository

import android.content.Context
import com.storyboy.core.AppConfig
import com.storyboy.data.GamebookStorage
import com.storyboy.data.StoreIndexParser
import com.storyboy.models.StoreGamebook
import java.net.HttpURLConnection
import java.net.URL

class GamebookStoreRepository(context: Context) {
    private val storage = GamebookStorage(context.applicationContext)

    fun listStoreGamebooks(): List<StoreGamebook> {
        val localIds = storage.listGamebookFiles()
            .mapNotNull { file -> runCatching { storage.readMetadata(file).id }.getOrNull() }
            .toSet()
        val json = getText(AppConfig.StoreIndexUrl)
        return StoreIndexParser.parse(json, localIds)
    }

    fun download(storeGamebook: StoreGamebook) {
        storage.downloadGamebook(storeGamebook.downloadUrl)
    }

    private fun getText(url: String): String {
        return URL(url).openConnection().asHttpConnection().use { connection ->
            connection.inputStream.bufferedReader().use { it.readText() }
        }
    }
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
