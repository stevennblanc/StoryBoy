package com.storyboy.engine

import com.storyboy.data.GamebookParser
import org.json.JSONArray
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

        nodes.validateTargets()

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
        val nodeId = nodeJson.optString("id").ifBlank {
            fallbackId ?: error("Story node is missing an id.")
        }
        val type = nodeJson.optString("type", "text")

        if (type == "lore") {
            return parseLoreNode(nodeJson, nodeId, type)
        }

        if (type == "puzzle") {
            return parsePuzzleNode(nodeJson, nodeId, type)
        }

        val choicesJson = nodeJson.optJSONArray("choices")
        val choices = parseChoices(choicesJson)

        return StoryNode(
            id = nodeId,
            type = type,
            text = nodeJson.optString("text"),
            choices = choices,
        )
    }

    private fun parseLoreNode(
        nodeJson: JSONObject,
        nodeId: String,
        type: String,
    ): StoryNode {
        val entriesJson = nodeJson.optJSONArray("entries")
        val text = buildString {
            if (entriesJson != null) {
                for (index in 0 until entriesJson.length()) {
                    val entryJson = entriesJson.getJSONObject(index)
                    if (isNotEmpty()) append("\n\n")
                    append(entryJson.optString("title"))
                    append("\n")
                    append(entryJson.optString("text"))
                }
            }
        }
        val returnTo = nodeJson.optString("return_to")
        val choices = if (returnTo.isBlank()) {
            emptyList()
        } else {
            listOf(StoryChoice(text = "Continue", targetNodeId = returnTo))
        }

        return StoryNode(
            id = nodeId,
            type = type,
            text = text,
            choices = choices,
        )
    }

    private fun parsePuzzleNode(
        nodeJson: JSONObject,
        nodeId: String,
        type: String,
    ): StoryNode {
        val answersJson = nodeJson.optJSONArray("answers")
        val answers = buildList {
            if (answersJson != null) {
                for (index in 0 until answersJson.length()) {
                    add(answersJson.getString(index).normalizeAnswer())
                }
            }
        }

        return StoryNode(
            id = nodeId,
            type = type,
            text = nodeJson.optString("question"),
            choices = emptyList(),
            acceptedAnswers = answers,
            correctTargetNodeId = nodeJson.optString("correct_target").ifBlank { null },
            incorrectTargetNodeId = nodeJson.optString("incorrect_target").ifBlank {
                nodeJson.optString("default_target").ifBlank { null }
            },
        )
    }

    private fun parseChoices(choicesJson: JSONArray?): List<StoryChoice> {
        return buildList {
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
    }

    private fun Map<String, StoryNode>.validateTargets() {
        values.forEach { node ->
            node.choices.forEach { choice ->
                require(containsKey(choice.targetNodeId)) {
                    "Choice from ${node.id} points to missing node ${choice.targetNodeId}."
                }
            }
            node.correctTargetNodeId?.let { target ->
                require(containsKey(target)) { "Puzzle ${node.id} points to missing correct target $target." }
            }
            node.incorrectTargetNodeId?.let { target ->
                require(containsKey(target)) { "Puzzle ${node.id} points to missing incorrect target $target." }
            }
        }
    }
}

fun String.normalizeAnswer(): String {
    return trim().lowercase().replace(Regex("\\s+"), " ")
}
