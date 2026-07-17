package com.storyboy.engine

data class StoryNode(
    val id: String,
    val type: String,
    val text: String,
    val choices: List<StoryChoice>,
    val evidenceGained: List<EvidenceItem> = emptyList(),
    val acceptedAnswers: List<String> = emptyList(),
    val correctTargetNodeId: String? = null,
    val incorrectTargetNodeId: String? = null,
)
