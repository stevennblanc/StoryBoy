package com.storyboy.engine

import com.storyboy.models.GamebookMetadata

data class StoryGamebook(
    val metadata: GamebookMetadata,
    val nodes: Map<String, StoryNode>,
    val evidenceCatalog: Map<String, EvidenceItem> = emptyMap(),
    val inventoryCatalog: Map<String, InventoryItem> = emptyMap(),
) {
    fun node(nodeId: String): StoryNode {
        return nodes[nodeId] ?: error("Story node not found: $nodeId")
    }
}
