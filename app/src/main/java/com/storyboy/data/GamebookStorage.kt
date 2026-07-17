package com.storyboy.data

import android.content.Context
import com.storyboy.core.AppConfig
import com.storyboy.models.GamebookArtwork
import com.storyboy.models.GamebookMetadata
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

class GamebookStorage(private val context: Context) {
    private val gamebooksDir: File
        get() = File(context.filesDir, AppConfig.GamebooksDirectory).apply { mkdirs() }

    private val artworkDir: File
        get() = File(context.filesDir, "gamebook_art").apply { mkdirs() }

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
        return GamebookParser.parseMetadata(readStoryJson(file))
    }

    fun readStoryJson(file: File): String {
        require(file.extension == AppConfig.GamebookExtension) {
            "StoryBoy only loads .${AppConfig.GamebookExtension} files."
        }
        return ZipFile(file).use { zipFile ->
            val storyEntry = zipFile.getEntry("story.json")
                ?: error("Gamebook package must contain story.json.")
            zipFile.getInputStream(storyEntry).bufferedReader().use { it.readText() }
        }
    }

    fun extractArtwork(file: File, metadata: GamebookMetadata): GamebookArtwork {
        val bookArtworkDir = File(artworkDir, metadata.id).apply { mkdirs() }
        var posterPath: String? = null
        var bannerPath: String? = null

        ZipFile(file).use { zipFile ->
            posterPath = zipFile.extractFirstMatching(
                candidates = listOf("poster.png", "poster.jpg", "cover.png", "cover.jpg"),
                targetDir = bookArtworkDir,
            )
            bannerPath = zipFile.extractFirstMatching(
                candidates = listOf("banner.png", "banner.jpg"),
                targetDir = bookArtworkDir,
            )
        }

        return GamebookArtwork(
            posterPath = posterPath,
            bannerPath = bannerPath,
        )
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

private fun ZipFile.extractFirstMatching(
    candidates: List<String>,
    targetDir: File,
): String? {
    val entry = candidates.firstNotNullOfOrNull { candidate -> getEntry(candidate) } ?: return null
    val targetFile = File(targetDir, entry.name.substringAfterLast('/'))
    getInputStream(entry).use { input ->
        targetFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return targetFile.absolutePath
}

private fun java.net.URLConnection.asHttpConnection(): HttpURLConnection {
    return (this as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        requestMethod = "GET"
        instanceFollowRedirects = true
        setRequestProperty("Accept", "application/octet-stream, application/zip")
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
