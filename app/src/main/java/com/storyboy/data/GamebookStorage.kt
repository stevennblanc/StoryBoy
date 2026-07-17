package com.storyboy.data

import android.content.Context
import com.storyboy.core.AppConfig
import com.storyboy.models.GamebookMetadata
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class GamebookStorage(private val context: Context) {
    private val gamebooksDir: File
        get() = File(context.filesDir, AppConfig.GamebooksDirectory).apply { mkdirs() }

    fun listGamebookFiles(): List<File> {
        return gamebooksDir
            .listFiles { file -> file.isFile && file.extension == AppConfig.GamebookExtension }
            ?.sortedBy { it.nameWithoutExtension.lowercase() }
            .orEmpty()
    }

    fun readMetadata(file: File): GamebookMetadata {
        require(file.extension == AppConfig.GamebookExtension) {
            "StoryBoy only loads .${AppConfig.GamebookExtension} files."
        }
        return GamebookParser.parseMetadata(file.readText())
    }

    fun downloadGamebook(url: String): File {
        val tempFile = File.createTempFile("storyboy-download", ".${AppConfig.GamebookExtension}", context.cacheDir)

        URL(url).openConnection().asHttpConnection().use { connection ->
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val metadata = readMetadata(tempFile)
        val targetFile = File(gamebooksDir, "${metadata.id}.${AppConfig.GamebookExtension}")
        tempFile.copyTo(targetFile, overwrite = true)
        tempFile.delete()
        return targetFile
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
            error("Gamebook download failed with HTTP $responseCode")
        }
        block(this)
    } finally {
        disconnect()
    }
}
