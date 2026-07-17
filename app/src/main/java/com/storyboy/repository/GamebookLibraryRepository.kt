package com.storyboy.repository

import android.content.Context
import com.storyboy.core.AppConfig
import com.storyboy.data.GamebookStorage
import com.storyboy.models.LocalGamebook

class GamebookLibraryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val storage = GamebookStorage(appContext)
    private val progressPrefs = appContext.getSharedPreferences(AppConfig.ProgressPreferences, Context.MODE_PRIVATE)

    fun listLocalGamebooks(): List<LocalGamebook> {
        return storage.listGamebookFiles().mapNotNull { file ->
            runCatching {
                val metadata = storage.readMetadata(file)
                val artwork = storage.extractArtwork(file, metadata)
                LocalGamebook(
                    metadata = metadata,
                    filePath = file.absolutePath,
                    posterPath = artwork.posterPath,
                    bannerPath = artwork.bannerPath,
                    hasPlaythroughInProgress = progressPrefs.contains(currentNodeKey(metadata.id)),
                )
            }.getOrNull()
        }
    }

    fun markPlaythroughStarted(gamebookId: String) {
        progressPrefs.edit().putBoolean(startedKey(gamebookId), true).apply()
    }

    private fun currentNodeKey(gamebookId: String): String = "current_node_$gamebookId"

    private fun startedKey(gamebookId: String): String = "started_$gamebookId"
}
