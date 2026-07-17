package com.storyboy.engine

import com.storyboy.data.GamebookParser
import org.json.JSONObject

object StoryGamebookParser {
    fun parse(json: String): StoryGamebook {
        val source = JSONObject(json)
        val metadata = GamebookParser.parseMetadata(json)
        val nodes = if (source.has("metadata")) {
            parseNodeArray(source.getJSONArray("nodes"))
        } else {
            parseNodeObject(source.getJSONObject("nodes"))
        }

        require(nodes.containsKey(metadata.startNodeId)) { "Start node does not exist." }

        return StoryGamebook(
            metadata = metadata,
            nodes = nodes,
        )
    }

    private fun parseNodeArray(nodesJson: org.json.JSONArray): Map<String, StoryNode> {
        return buildMap {
            for (index in 0 until nodesJson.length()) {
                val nodeJson = nodesJson.getJSONObject(index)
                val node = parseNode(nodeJson)
                put(node.id, node)
            }
        }
    }

    private fun parseNodeObject(nodesJson: JSONObject): Map<String, StoryNode> {
        return buildMap {
            nodesJson.keys().forEach { id ->
                val nodeJson = nodesJson.getJSONObject(id)
                val node = parseNode(nodeJson, fallbackId = id)
                put(node.id, node)
            }
        }
    }

    private fun parseNode(nodeJson: JSONObject, fallbackId: String? = null): StoryNode {
        val choicesJson = nodeJson.optJSONArray("choices")
        val choices = buildList {
            if (choicesJson != null) {
                for (index in 0 until choicesJson.length()) {
                    val choiceJson = choicesJson.getJSONObject(index)
                    add(
                        StoryChoice(
                            text = choiceJson.getString("text"),
                            targetNodeId = choiceJson.getString("target"),
                        ),
                    )
                }
            }
        }

        return StoryNode(
            id = nodeJson.optString("id", fallbackId ?: error("Story node is missing an id.")),
            type = nodeJson.optString("type", "text"),
            text = nodeJson.optString("text"),
            choices = choices,
        )
    }
}
