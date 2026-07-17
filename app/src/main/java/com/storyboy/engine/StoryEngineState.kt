package com.storyboy.engine

data class StoryEngineState(
    val isLoading: Boolean = true,
    val gamebook: StoryGamebook? = null,
    val currentNode: StoryNode? = null,
    val error: String? = null,
)
