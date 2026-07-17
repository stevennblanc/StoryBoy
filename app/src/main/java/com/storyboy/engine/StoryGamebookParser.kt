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
        val evidenceCatalog = parseEvidenceCatalog(source, nodes)
        val inventoryCatalog = parseInventoryCatalog(source, nodes)

        return StoryGamebook(
            metadata = metadata,
            nodes = nodes,
            evidenceCatalog = evidenceCatalog,
            inventoryCatalog = inventoryCatalog,
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

        if (type == "inventory") {
            return parseCollectionNode(nodeJson, nodeId, type, defaultContinueText = "Continue")
        }

        if (type == "evidence") {
            return parseCollectionNode(nodeJson, nodeId, type, defaultContinueText = "Continue")
        }

        if (type == "map") {
            return parseMapNode(nodeJson, nodeId, type)
        }

        if (type == "battle") {
            return parseBattleNode(nodeJson, nodeId, type)
        }

        val choicesJson = nodeJson.optJSONArray("choices")
        val choices = parseChoices(choicesJson)

        return StoryNode(
            id = nodeId,
            type = type,
            text = nodeJson.optString("text"),
            images = parseImages(nodeJson),
            choices = choices,
            evidenceGained = parseEvidenceGained(nodeJson),
            inventoryGained = parseInventoryGained(nodeJson),
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
            images = parseImages(nodeJson),
            choices = choices,
            evidenceGained = parseEvidenceGained(nodeJson),
            inventoryGained = parseInventoryGained(nodeJson),
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
            images = parseImages(nodeJson),
            choices = emptyList(),
            evidenceGained = parseEvidenceGained(nodeJson),
            inventoryGained = parseInventoryGained(nodeJson),
            acceptedAnswers = answers,
            correctTargetNodeId = nodeJson.optString("correct_target").ifBlank { null },
            incorrectTargetNodeId = nodeJson.optString("incorrect_target").ifBlank {
                nodeJson.optString("default_target").ifBlank { null }
            },
        )
    }

    private fun parseCollectionNode(
        nodeJson: JSONObject,
        nodeId: String,
        type: String,
        defaultContinueText: String,
    ): StoryNode {
        val returnTo = nodeJson.optString("return_to")
        val choices = parseChoices(nodeJson.optJSONArray("choices")).ifEmpty {
            if (returnTo.isBlank()) {
                emptyList()
            } else {
                listOf(StoryChoice(text = defaultContinueText, targetNodeId = returnTo))
            }
        }

        return StoryNode(
            id = nodeId,
            type = type,
            text = nodeJson.optString("text"),
            images = parseImages(nodeJson),
            choices = choices,
            evidenceGained = parseEvidenceGained(nodeJson),
            inventoryGained = parseInventoryGained(nodeJson),
        )
    }

    private fun parseMapNode(
        nodeJson: JSONObject,
        nodeId: String,
        type: String,
    ): StoryNode {
        return StoryNode(
            id = nodeId,
            type = type,
            text = nodeJson.optString("text", nodeJson.optString("title", "Choose a location.")),
            images = parseImages(nodeJson),
            choices = parseChoices(nodeJson.optJSONArray("choices")),
            evidenceGained = parseEvidenceGained(nodeJson),
            inventoryGained = parseInventoryGained(nodeJson),
            mapLocations = parseMapLocations(nodeJson.optJSONArray("locations")),
        )
    }

    private fun parseBattleNode(
        nodeJson: JSONObject,
        nodeId: String,
        type: String,
    ): StoryNode {
        val battleJson = nodeJson.optJSONObject("battle") ?: nodeJson
        val winTarget = battleJson.optAnyString("win_target", "on_win", "success_target")
        val loseTarget = battleJson.optAnyString("lose_target", "on_lose", "failure_target")
        val drawTarget = battleJson.optAnyString("draw_target", "tie_target", "on_draw").ifBlank { null }
        val playerDice = battleJson.optAnyString("player_dice", "playerDice").ifBlank { "1d6" }
        val opponentDice = battleJson.optAnyString("opponent_dice", "opponentDice", "enemy_dice", "enemyDice").ifBlank { "1d6" }

        validateDiceExpression(playerDice, nodeId)
        validateDiceExpression(opponentDice, nodeId)

        return StoryNode(
            id = nodeId,
            type = type,
            text = nodeJson.optString("text", nodeJson.optString("title", "Roll for the outcome.")),
            images = parseImages(nodeJson),
            choices = emptyList(),
            evidenceGained = parseEvidenceGained(nodeJson),
            inventoryGained = parseInventoryGained(nodeJson),
            battle = BattleConfig(
                playerDice = playerDice,
                opponentDice = opponentDice,
                playerBonus = battleJson.optAnyInt("player_bonus", "playerBonus", default = 0),
                opponentBonus = battleJson.optAnyInt(
                    "opponent_bonus",
                    "opponentBonus",
                    "enemy_bonus",
                    "enemyBonus",
                    default = 0,
                ),
                winTargetNodeId = winTarget,
                loseTargetNodeId = loseTarget,
                drawTargetNodeId = drawTarget,
                itemModifiers = parseBattleModifiers(
                    battleJson.optJSONArray("item_modifiers")
                        ?: battleJson.optJSONArray("inventory_modifiers"),
                ),
            ),
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

    private fun parseImages(nodeJson: JSONObject): List<StoryImage> {
        val singleImage = nodeJson.optString("image").ifBlank { null }
        val imagesJson = nodeJson.optJSONArray("images")

        return buildList {
            if (singleImage != null) {
                add(StoryImage(path = singleImage, caption = nodeJson.optString("image_caption")))
            }
            if (imagesJson != null) {
                for (index in 0 until imagesJson.length()) {
                    val image = imagesJson.get(index)
                    if (image is JSONObject) {
                        add(
                            StoryImage(
                                path = image.getString("path"),
                                caption = image.optString("caption"),
                            ),
                        )
                    } else {
                        add(StoryImage(path = image.toString()))
                    }
                }
            }
        }
    }

    private fun parseEvidenceCatalog(
        source: JSONObject,
        nodes: Map<String, StoryNode>,
    ): Map<String, EvidenceItem> {
        val catalog = linkedMapOf<String, EvidenceItem>()
        val evidenceJson = source.optJSONArray("evidence")
        if (evidenceJson != null) {
            for (index in 0 until evidenceJson.length()) {
                val item = evidenceJson.getEvidenceItem(index)
                catalog[item.id] = item
            }
        }
        nodes.values
            .flatMap { it.evidenceGained }
            .forEach { item -> catalog[item.id] = item }
        return catalog
    }

    private fun parseInventoryCatalog(
        source: JSONObject,
        nodes: Map<String, StoryNode>,
    ): Map<String, InventoryItem> {
        val catalog = linkedMapOf<String, InventoryItem>()
        val inventoryJson = source.optJSONArray("inventory")
        if (inventoryJson != null) {
            for (index in 0 until inventoryJson.length()) {
                val item = inventoryJson.getInventoryItem(index)
                catalog[item.id] = item
            }
        }
        nodes.values
            .flatMap { it.inventoryGained }
            .forEach { item -> catalog[item.id] = item }
        return catalog
    }

    private fun parseEvidenceGained(nodeJson: JSONObject): List<EvidenceItem> {
        val evidenceJson = nodeJson.optJSONArray("evidence")
            ?: nodeJson.optJSONArray("gain_evidence")
            ?: nodeJson.optJSONArray("gains_evidence")
            ?: return emptyList()

        return buildList {
            for (index in 0 until evidenceJson.length()) {
                add(evidenceJson.getEvidenceItem(index))
            }
        }
    }

    private fun parseInventoryGained(nodeJson: JSONObject): List<InventoryItem> {
        val inventoryJson = nodeJson.optJSONArray("inventory")
            ?: nodeJson.optJSONArray("items")
            ?: nodeJson.optJSONArray("gain_inventory")
            ?: nodeJson.optJSONArray("gains_inventory")
            ?: return emptyList()

        return buildList {
            for (index in 0 until inventoryJson.length()) {
                add(inventoryJson.getInventoryItem(index))
            }
        }
    }

    private fun parseMapLocations(locationsJson: JSONArray?): List<MapLocation> {
        return buildList {
            if (locationsJson != null) {
                for (index in 0 until locationsJson.length()) {
                    val locationJson = locationsJson.getJSONObject(index)
                    add(
                        MapLocation(
                            title = locationJson.getString("title"),
                            description = locationJson.optString("description"),
                            targetNodeId = locationJson.getString("target"),
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
            node.mapLocations.forEach { location ->
                require(containsKey(location.targetNodeId)) {
                    "Map node ${node.id} points to missing location target ${location.targetNodeId}."
                }
            }
            node.battle?.let { battle ->
                require(containsKey(battle.winTargetNodeId)) {
                    "Battle ${node.id} points to missing win target ${battle.winTargetNodeId}."
                }
                require(containsKey(battle.loseTargetNodeId)) {
                    "Battle ${node.id} points to missing lose target ${battle.loseTargetNodeId}."
                }
                battle.drawTargetNodeId?.let { target ->
                    require(containsKey(target)) { "Battle ${node.id} points to missing draw target $target." }
                }
            }
        }
    }
}

private fun parseBattleModifiers(modifiersJson: JSONArray?): List<BattleModifier> {
    return buildList {
        if (modifiersJson != null) {
            for (index in 0 until modifiersJson.length()) {
                val modifierJson = modifiersJson.getJSONObject(index)
                val itemId = modifierJson.optAnyString("item", "item_id", "inventory_id", "requires_item", "id")
                add(
                    BattleModifier(
                        itemId = itemId,
                        bonus = modifierJson.optAnyInt("bonus", "player_bonus", default = 0),
                        description = modifierJson.optString("description", itemId.toDisplayTitle()),
                    ),
                )
            }
        }
    }
}

private fun validateDiceExpression(expression: String, nodeId: String) {
    val match = DiceExpressionRegex.matchEntire(expression.trim())
    require(match != null) {
        "Battle $nodeId uses invalid dice expression $expression. Use forms like 1d6 or 2d8."
    }
    val diceCount = match.groupValues[1].ifBlank { "1" }.toInt()
    val sides = match.groupValues[2].toInt()
    require(diceCount in 1..20 && sides in 2..100) {
        "Battle $nodeId uses unsupported dice expression $expression."
    }
}

private val DiceExpressionRegex = Regex("""(\d*)d(\d+)""", RegexOption.IGNORE_CASE)

private fun JSONObject.optAnyString(vararg names: String): String {
    names.forEach { name ->
        val value = optString(name)
        if (value.isNotBlank()) return value
    }
    return ""
}

private fun JSONObject.optAnyInt(vararg names: String, default: Int): Int {
    names.forEach { name ->
        if (has(name)) return optInt(name, default)
    }
    return default
}

private fun JSONArray.getEvidenceItem(index: Int): EvidenceItem {
    val rawItem = get(index)
    return if (rawItem is JSONObject) {
        val id = rawItem.getString("id")
        EvidenceItem(
            id = id,
            title = rawItem.optString("title", id.toEvidenceTitle()),
            description = rawItem.optString("description"),
        )
    } else {
        val id = rawItem.toString()
        EvidenceItem(
            id = id,
            title = id.toEvidenceTitle(),
            description = "",
        )
    }
}

private fun JSONArray.getInventoryItem(index: Int): InventoryItem {
    val rawItem = get(index)
    return if (rawItem is JSONObject) {
        val id = rawItem.getString("id")
        InventoryItem(
            id = id,
            title = rawItem.optString("title", id.toDisplayTitle()),
            description = rawItem.optString("description"),
        )
    } else {
        val id = rawItem.toString()
        InventoryItem(
            id = id,
            title = id.toDisplayTitle(),
            description = "",
        )
    }
}

fun String.normalizeAnswer(): String {
    return trim().lowercase().replace(Regex("\\s+"), " ")
}

private fun String.toEvidenceTitle(): String {
    return toDisplayTitle()
}

private fun String.toDisplayTitle(): String {
    return replace(Regex("[_\\-]+"), " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }
}
