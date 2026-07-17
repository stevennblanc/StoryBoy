package com.storyboy.engine

sealed interface StoryPresentationEvent {
    data class NodeEntered(val nodeId: String) : StoryPresentationEvent

    data class InventoryGained(val items: List<InventoryItem>) : StoryPresentationEvent

    data class EvidenceGained(val evidence: List<EvidenceItem>) : StoryPresentationEvent

    data class BattleResolved(val result: BattleResult) : StoryPresentationEvent

    data class PuzzleAnswered(
        val correct: Boolean,
        val targetNodeId: String,
    ) : StoryPresentationEvent
}
