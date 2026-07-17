package com.storyboy.engine

data class BattleConfig(
    val playerDice: String = "1d6",
    val opponentDice: String = "1d6",
    val playerBonus: Int = 0,
    val opponentBonus: Int = 0,
    val winTargetNodeId: String,
    val loseTargetNodeId: String,
    val drawTargetNodeId: String? = null,
    val itemModifiers: List<BattleModifier> = emptyList(),
)

data class BattleModifier(
    val itemId: String,
    val bonus: Int,
    val description: String,
)

data class BattleRoll(
    val expression: String,
    val rolls: List<Int>,
    val bonus: Int,
    val total: Int,
)

data class BattleResult(
    val playerRoll: BattleRoll,
    val opponentRoll: BattleRoll,
    val appliedModifiers: List<BattleModifier>,
    val outcome: BattleOutcome,
    val targetNodeId: String,
)

enum class BattleOutcome {
    Win,
    Lose,
    Draw,
}
