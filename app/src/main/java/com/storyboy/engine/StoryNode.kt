package com.storyboy.engine

data class StoryNode(
    val id: String,
    val type: String,
    val text: String,
    val images: List<StoryImage>,
    val choices: List<StoryChoice>,
    val evidenceGained: List<EvidenceItem> = emptyList(),
    val inventoryGained: List<InventoryItem> = emptyList(),
    val mapLocations: List<MapLocation> = emptyList(),
    val acceptedAnswers: List<String> = emptyList(),
    val correctTargetNodeId: String? = null,
    val incorrectTargetNodeId: String? = null,
)
