package com.storyboy.engine

data class StoryNode(
    val id: String,
    val type: String,
    val text: String,
    val choices: List<StoryChoice>,
)
