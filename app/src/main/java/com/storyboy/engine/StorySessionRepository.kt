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

    fun extractStoryAsset(gamebookPath: String, gamebookId: String, assetPath: String): String? {
        return storage.extractStoryAsset(gamebookPath, gamebookId, assetPath)
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

    fun collectedEvidenceIds(gamebookId: String): Set<String> {
        return progressPrefs.getStringSet(evidenceKey(gamebookId), emptySet()).orEmpty()
    }

    fun saveEvidence(gamebookId: String, evidenceIds: Set<String>) {
        progressPrefs.edit()
            .putStringSet(evidenceKey(gamebookId), evidenceIds)
            .apply()
    }

    fun collectedInventoryIds(gamebookId: String): Set<String> {
        return progressPrefs.getStringSet(inventoryKey(gamebookId), emptySet()).orEmpty()
    }

    fun saveInventory(gamebookId: String, inventoryIds: Set<String>) {
        progressPrefs.edit()
            .putStringSet(inventoryKey(gamebookId), inventoryIds)
            .apply()
    }

    fun reset(gamebookId: String) {
        progressPrefs.edit()
            .remove(currentNodeKey(gamebookId))
            .remove(startedKey(gamebookId))
            .remove(evidenceKey(gamebookId))
            .remove(inventoryKey(gamebookId))
            .apply()
    }

    private fun currentNodeKey(gamebookId: String): String = "current_node_$gamebookId"

    private fun startedKey(gamebookId: String): String = "started_$gamebookId"

    private fun evidenceKey(gamebookId: String): String = "evidence_$gamebookId"

    private fun inventoryKey(gamebookId: String): String = "inventory_$gamebookId"
}
