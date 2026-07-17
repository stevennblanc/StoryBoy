package com.storyboy.data

import com.storyboy.models.GamebookMetadata
import org.json.JSONObject

object GamebookParser {
    fun parseMetadata(json: String): GamebookMetadata {
        val source = JSONObject(json)
        val format = source.optString("format")
        val formatVersion = source.optInt("formatVersion")
        val nodes = source.optJSONObject("nodes")

        require(format == "storyboy.gamebook") { "Unsupported gamebook format." }
        require(formatVersion >= 1) { "Unsupported gamebook format version." }
        require(nodes != null && nodes.length() > 0) { "Gamebook must contain nodes." }

        val startNodeId = source.getString("startNodeId")
        require(nodes.has(startNodeId)) { "Start node does not exist." }

        return GamebookMetadata(
            id = source.getString("id"),
            title = source.getString("title"),
            author = source.optString("author", "Unknown author"),
            version = source.optString("version", "1.0.0"),
            description = source.optString("description"),
            startNodeId = startNodeId,
        )
    }
}
