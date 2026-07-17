package com.storyboy.engine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoryEngineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StorySessionRepository(application)
    private val mutableState = MutableStateFlow(StoryEngineState())

    val state: StateFlow<StoryEngineState> = mutableState.asStateFlow()

    fun load(gamebookPath: String) {
        mutableState.value = StoryEngineState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.load(gamebookPath)
                }
            }.onSuccess { gamebook ->
                val nodeId = repository.currentNodeId(gamebook)
                repository.saveCurrentNode(gamebook.metadata.id, nodeId)
                mutableState.value = StoryEngineState(
                    isLoading = false,
                    gamebook = gamebook,
                    currentNode = gamebook.node(nodeId),
                )
            }.onFailure { throwable ->
                mutableState.value = StoryEngineState(
                    isLoading = false,
                    error = throwable.message ?: "Unable to load gamebook",
                )
            }
        }
    }

    fun choose(choice: StoryChoice) {
        val gamebook = mutableState.value.gamebook ?: return
        val targetNode = runCatching { gamebook.node(choice.targetNodeId) }.getOrElse { return }
        repository.saveCurrentNode(gamebook.metadata.id, targetNode.id)
        mutableState.update { it.copy(currentNode = targetNode) }
    }

    fun restart() {
        val gamebook = mutableState.value.gamebook ?: return
        repository.reset(gamebook.metadata.id)
        repository.saveCurrentNode(gamebook.metadata.id, gamebook.metadata.startNodeId)
        mutableState.update { it.copy(currentNode = gamebook.node(gamebook.metadata.startNodeId)) }
    }
}
