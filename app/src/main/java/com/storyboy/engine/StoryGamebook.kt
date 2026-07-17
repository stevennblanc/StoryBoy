package com.storyboy.engine

import com.storyboy.models.GamebookMetadata

data class StoryGamebook(
    val metadata: GamebookMetadata,
    val nodes: Map<String, StoryNode>,
) {
    fun node(nodeId: String): StoryNode {
        return nodes[nodeId] ?: error("Story node not found: $nodeId")
    }
}
