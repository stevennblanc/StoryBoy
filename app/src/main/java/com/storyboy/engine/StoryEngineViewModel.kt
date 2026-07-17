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
import kotlin.random.Random

class StoryEngineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StorySessionRepository(application)
    private val mutableState = MutableStateFlow(StoryEngineState())
    private var currentGamebookPath: String = ""

    val state: StateFlow<StoryEngineState> = mutableState.asStateFlow()

    fun load(gamebookPath: String) {
        currentGamebookPath = gamebookPath
        mutableState.value = StoryEngineState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.load(gamebookPath)
                }
            }.onSuccess { gamebook ->
                val nodeId = repository.currentNodeId(gamebook)
                enterNode(gamebook = gamebook, node = gamebook.node(nodeId))
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
        enterNode(gamebook = gamebook, node = targetNode)
    }

    fun submitPuzzleAnswer(answer: String) {
        val gamebook = mutableState.value.gamebook ?: return
        val node = mutableState.value.currentNode ?: return
        val targetNodeId = if (answer.normalizeAnswer() in node.acceptedAnswers) {
            node.correctTargetNodeId
        } else {
            node.incorrectTargetNodeId
        } ?: return
        val targetNode = runCatching { gamebook.node(targetNodeId) }.getOrElse { return }
        enterNode(gamebook = gamebook, node = targetNode)
    }

    fun rollBattle() {
        val node = mutableState.value.currentNode ?: return
        val battle = node.battle ?: return
        val inventoryIds = mutableState.value.collectedInventory.map { it.id }.toSet()
        val appliedModifiers = battle.itemModifiers.filter { it.itemId in inventoryIds }
        val itemBonus = appliedModifiers.sumOf { it.bonus }
        val playerRoll = rollDice(
            expression = battle.playerDice,
            bonus = battle.playerBonus + itemBonus,
        )
        val opponentRoll = rollDice(
            expression = battle.opponentDice,
            bonus = battle.opponentBonus,
        )
        val outcome = when {
            playerRoll.total > opponentRoll.total -> BattleOutcome.Win
            playerRoll.total < opponentRoll.total -> BattleOutcome.Lose
            else -> BattleOutcome.Draw
        }
        val targetNodeId = when (outcome) {
            BattleOutcome.Win -> battle.winTargetNodeId
            BattleOutcome.Lose -> battle.loseTargetNodeId
            BattleOutcome.Draw -> battle.drawTargetNodeId ?: battle.winTargetNodeId
        }

        mutableState.update { state ->
            state.copy(
                currentBattleResult = BattleResult(
                    playerRoll = playerRoll,
                    opponentRoll = opponentRoll,
                    appliedModifiers = appliedModifiers,
                    outcome = outcome,
                    targetNodeId = targetNodeId,
                ),
            )
        }
    }

    fun restart() {
        val gamebook = mutableState.value.gamebook ?: return
        repository.reset(gamebook.metadata.id)
        enterNode(gamebook = gamebook, node = gamebook.node(gamebook.metadata.startNodeId))
    }

    private fun enterNode(gamebook: StoryGamebook, node: StoryNode) {
        val collectedIds = repository.collectedEvidenceIds(gamebook.metadata.id)
            .plus(node.evidenceGained.map { it.id })
        val inventoryIds = repository.collectedInventoryIds(gamebook.metadata.id)
            .plus(node.inventoryGained.map { it.id })
        repository.saveCurrentNode(gamebook.metadata.id, node.id)
        repository.saveEvidence(gamebook.metadata.id, collectedIds)
        repository.saveInventory(gamebook.metadata.id, inventoryIds)
        mutableState.value = StoryEngineState(
            isLoading = false,
            gamebook = gamebook,
            currentNode = node,
            currentNodeImages = node.images.map { image ->
                image.copy(
                    path = repository.extractStoryAsset(
                        gamebookPath = currentGamebookPath,
                        gamebookId = gamebook.metadata.id,
                        assetPath = image.path,
                    ) ?: image.path,
                )
            },
            collectedEvidence = collectedIds.mapNotNull { gamebook.evidenceCatalog[it] },
            collectedInventory = inventoryIds.mapNotNull { gamebook.inventoryCatalog[it] },
            currentBattleResult = null,
        )
    }

    private fun rollDice(expression: String, bonus: Int): BattleRoll {
        val match = DiceExpressionRegex.matchEntire(expression.trim())
            ?: return BattleRoll(expression = expression, rolls = emptyList(), bonus = bonus, total = bonus)
        val diceCount = match.groupValues[1].ifBlank { "1" }.toInt()
        val sides = match.groupValues[2].toInt()
        val rolls = List(diceCount) { Random.nextInt(from = 1, until = sides + 1) }
        return BattleRoll(
            expression = expression,
            rolls = rolls,
            bonus = bonus,
            total = rolls.sum() + bonus,
        )
    }

    companion object {
        private val DiceExpressionRegex = Regex("""(\d*)d(\d+)""", RegexOption.IGNORE_CASE)
    }
}
