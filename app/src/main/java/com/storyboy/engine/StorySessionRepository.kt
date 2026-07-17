package com.storyboy.engine

import android.content.Context
import com.storyboy.core.AppConfig
import com.storyboy.data.GamebookStorage
import java.io.File

class StorySessionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val storage = GamebookStorage(appContext)
    private val progressPrefs = appContext.getSharedPreferences(AppConfig.ProgressPreferences, Context.MODE_PRIVATE)

    fun load(gamebookPath: String): StoryGamebook {
        return StoryGamebookParser.parse(storage.readStoryJson(File(gamebookPath)))
    }

    fun currentNodeId(gamebook: StoryGamebook): String {
        return progressPrefs.getString(currentNodeKey(gamebook.metadata.id), null)
            ?.takeIf { gamebook.nodes.containsKey(it) }
            ?: gamebook.metadata.startNodeId
    }

    fun saveCurrentNode(gamebookId: String, nodeId: String) {
        progressPrefs.edit()
            .putString(currentNodeKey(gamebookId), nodeId)
            .putBoolean(startedKey(gamebookId), true)
            .apply()
    }

    fun reset(gamebookId: String) {
        progressPrefs.edit()
            .remove(currentNodeKey(gamebookId))
            .remove(startedKey(gamebookId))
            .apply()
    }

    private fun currentNodeKey(gamebookId: String): String = "current_node_$gamebookId"

    private fun startedKey(gamebookId: String): String = "started_$gamebookId"
}
