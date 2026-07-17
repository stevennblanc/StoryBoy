package com.storyboy.data

import com.storyboy.models.GamebookMetadata
import org.json.JSONObject

object GamebookParser {
    fun parseMetadata(json: String): GamebookMetadata {
        val source = JSONObject(json)
        return if (source.has("metadata")) {
            parsePackagedMetadata(source)
        } else {
            parseFlatMetadata(source)
        }
    }

    private fun parsePackagedMetadata(source: JSONObject): GamebookMetadata {
        val metadata = source.getJSONObject("metadata")
        val nodes = source.optJSONArray("nodes")

        require(nodes != null && nodes.length() > 0) { "Gamebook must contain nodes." }

        val startNodeId = metadata.getString("start_node")
        require(nodes.containsNode(startNodeId)) { "Start node does not exist." }

        return GamebookMetadata(
            id = metadata.optString("folder", metadata.getString("title").toStableId()),
            title = metadata.getString("title"),
            author = metadata.optString("author", "Unknown author"),
            genre = metadata.optString("genre", "Gamebook"),
            version = metadata.optString("version", "1.0.0"),
            description = metadata.optString("description"),
            startNodeId = startNodeId,
        )
    }

    private fun parseFlatMetadata(source: JSONObject): GamebookMetadata {
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
            genre = source.optString("genre", "Gamebook"),
            version = source.optString("version", "1.0.0"),
            description = source.optString("description"),
            startNodeId = startNodeId,
        )
    }
}

private fun org.json.JSONArray.containsNode(nodeId: String): Boolean {
    for (index in 0 until length()) {
        if (optJSONObject(index)?.optString("id") == nodeId) {
            return true
        }
    }
    return false
}

private fun String.toStableId(): String {
    return lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
}
