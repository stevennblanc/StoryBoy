package com.storyboy

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.storyboy.core.Navigation
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig
import com.storyboy.engine.BattleConfig
import com.storyboy.engine.BattleOutcome
import com.storyboy.engine.BattleResult
import com.storyboy.engine.EvidenceItem
import com.storyboy.engine.InventoryItem
import com.storyboy.engine.MapLocation
import com.storyboy.engine.StoryChoice
import com.storyboy.engine.StoryEngineState
import com.storyboy.engine.StoryEngineViewModel
import com.storyboy.engine.StoryImage

class StoryEngineActivity : ComponentActivity() {
    private val viewModel: StoryEngineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gamebookPath = intent.getStringExtra(Navigation.ExtraGamebookPath).orEmpty()

        setContent {
            ThemeManager.StoryBoyTheme {
                val state by viewModel.state.collectAsState()

                LaunchedEffect(gamebookPath) {
                    viewModel.load(gamebookPath)
                }

                StoryReaderScreen(
                    state = state,
                    onBack = ::finish,
                    onRestart = viewModel::restart,
                    onChoice = viewModel::choose,
                    onPuzzleAnswer = viewModel::submitPuzzleAnswer,
                    onBattleRoll = viewModel::rollBattle,
                )
            }
        }
    }
}

@Composable
private fun StoryReaderScreen(
    state: StoryEngineState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onChoice: (StoryChoice) -> Unit,
    onPuzzleAnswer: (String) -> Unit,
    onBattleRoll: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UiConfig.ThemeColors.ReaderPageCol)
            .padding(UiConfig.Spacing.ScreenPadding),
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            state.error != null -> ReaderError(message = state.error, onBack = onBack)
            state.gamebook != null && state.currentNode != null -> ReaderContent(
                state = state,
                onBack = onBack,
                onRestart = onRestart,
                onChoice = onChoice,
                onPuzzleAnswer = onPuzzleAnswer,
                onBattleRoll = onBattleRoll,
            )
        }
    }
}

@Composable
private fun ReaderContent(
    state: StoryEngineState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onChoice: (StoryChoice) -> Unit,
    onPuzzleAnswer: (String) -> Unit,
    onBattleRoll: () -> Unit,
) {
    val gamebook = state.gamebook ?: return
    val node = state.currentNode ?: return
    var showEvidence by remember { mutableStateOf(false) }
    var showInventory by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
    ) {
        ReaderTopBar(
            title = gamebook.metadata.title,
            evidenceCount = state.collectedEvidence.size,
            inventoryCount = state.collectedInventory.size,
            onBack = onBack,
            onRestart = onRestart,
            onEvidence = { showEvidence = !showEvidence },
            onInventory = { showInventory = !showInventory },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.SectionGap),
        ) {
            if (showEvidence) {
                EvidenceBoard(evidence = state.collectedEvidence)
            }

            if (showInventory) {
                InventoryPanel(inventory = state.collectedInventory)
            }

            state.currentNodeImages.forEach { image ->
                StoryInlineImage(image = image)
            }

            Text(
                text = node.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = UiConfig.ThemeColors.ReaderText,
                    fontFamily = UiConfig.Fonts.SecondaryFontFamily,
                    lineHeight = UiConfig.Fonts.ReaderLineHeight,
                ),
            )

            if (node.type == "puzzle") {
                PuzzleInput(
                    nodeId = node.id,
                    question = node.text,
                    onSubmit = onPuzzleAnswer,
                )
            } else if (node.type == "map") {
                MapPicker(
                    locations = node.mapLocations,
                    onChoice = onChoice,
                )
            } else if (node.type == "battle" && node.battle != null) {
                BattlePanel(
                    battle = node.battle,
                    inventory = state.collectedInventory,
                    result = state.currentBattleResult,
                    onRoll = onBattleRoll,
                    onChoice = onChoice,
                )
            } else if (node.choices.isEmpty()) {
                Text(
                    text = "The End",
                    style = MaterialTheme.typography.headlineMedium.copy(color = UiConfig.ThemeColors.ReaderText),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer)) {
                    node.choices.forEach { choice ->
                        ChoiceButton(choice = choice, onChoice = onChoice)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryInlineImage(image: StoryImage) {
    val bitmap = remember(image.path) {
        BitmapFactory.decodeFile(image.path)
    }

    if (bitmap != null) {
        Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = image.caption.ifBlank { null },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = UiConfig.Controls.FocusThickness,
                        color = UiConfig.ThemeColors.ReaderDivider,
                        shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
                    ),
                contentScale = ContentScale.FillWidth,
            )
            if (image.caption.isNotBlank()) {
                Text(
                    text = image.caption,
                    style = MaterialTheme.typography.bodyMedium.copy(color = UiConfig.ThemeColors.ReaderMutedText),
                )
            }
        }
    }
}

@Composable
private fun BattlePanel(
    battle: BattleConfig,
    inventory: List<InventoryItem>,
    result: BattleResult?,
    onRoll: () -> Unit,
    onChoice: (StoryChoice) -> Unit,
) {
    val inventoryIds = inventory.map { it.id }.toSet()

    Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer)) {
        ReaderCollectionPanel(title = "Battle") {
            BattleLine(label = "Your roll", value = battle.playerDice.withBonus(battle.playerBonus))
            BattleLine(label = "Opponent", value = battle.opponentDice.withBonus(battle.opponentBonus))
            battle.itemModifiers.forEach { modifier ->
                val isActive = modifier.itemId in inventoryIds
                BattleLine(
                    label = if (isActive) "Prepared" else "Missing item",
                    value = "${modifier.description} (${modifier.bonus.signedBonus()})",
                )
            }
        }

        if (result == null) {
            Button(
                onClick = onRoll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = UiConfig.ThemeColors.ReaderChoiceCol,
                    contentColor = UiConfig.ThemeColors.ReaderText,
                ),
            ) {
                Text("Roll")
            }
        } else {
            BattleResultPanel(result = result)
            ChoiceButton(
                choice = StoryChoice(
                    text = "Continue",
                    targetNodeId = result.targetNodeId,
                ),
                onChoice = onChoice,
            )
        }
    }
}

@Composable
private fun BattleLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = UiConfig.ThemeColors.ReaderMutedText),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(color = UiConfig.ThemeColors.ReaderText),
        )
    }
}

@Composable
private fun BattleResultPanel(result: BattleResult) {
    ReaderCollectionPanel(title = result.outcome.displayLabel()) {
        CollectionItem(
            title = "You: ${result.playerRoll.rolls.joinToString(" + ")} ${result.playerRoll.bonus.signedBonus()}",
            description = "Total ${result.playerRoll.total}",
        )
        CollectionItem(
            title = "Opponent: ${result.opponentRoll.rolls.joinToString(" + ")} ${result.opponentRoll.bonus.signedBonus()}",
            description = "Total ${result.opponentRoll.total}",
        )
        if (result.appliedModifiers.isNotEmpty()) {
            CollectionItem(
                title = "Preparation helped",
                description = result.appliedModifiers.joinToString { it.description },
            )
        }
    }
}

private fun String.withBonus(bonus: Int): String {
    return if (bonus == 0) this else "$this ${bonus.signedBonus()}"
}

private fun Int.signedBonus(): String {
    return if (this >= 0) "+$this" else toString()
}

private fun BattleOutcome.displayLabel(): String {
    return when (this) {
        BattleOutcome.Win -> "Success"
        BattleOutcome.Lose -> "Setback"
        BattleOutcome.Draw -> "Draw"
    }
}

@Composable
private fun PuzzleInput(
    nodeId: String,
    question: String,
    onSubmit: (String) -> Unit,
) {
    var answer by remember(nodeId) { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer)) {
        Text(
            text = question,
            style = MaterialTheme.typography.headlineMedium.copy(color = UiConfig.ThemeColors.ReaderText),
        )
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Answer") },
        )
        Button(
            onClick = { onSubmit(answer) },
            enabled = answer.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = UiConfig.ThemeColors.ReaderChoiceCol,
                contentColor = UiConfig.ThemeColors.ReaderText,
            ),
        ) {
            Text("Submit")
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    evidenceCount: Int,
    inventoryCount: Int,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onEvidence: () -> Unit,
    onInventory: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text("Library")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(color = UiConfig.ThemeColors.ReaderMutedText),
        )
        Row {
            TextButton(onClick = onInventory, enabled = inventoryCount > 0) {
                Text("Items $inventoryCount")
            }
            TextButton(onClick = onEvidence, enabled = evidenceCount > 0) {
                Text("Evidence $evidenceCount")
            }
            TextButton(onClick = onRestart) {
                Text("Restart")
            }
        }
    }
}

@Composable
private fun InventoryPanel(inventory: List<InventoryItem>) {
    ReaderCollectionPanel(title = "Inventory") {
        inventory.forEach { item ->
            CollectionItem(title = item.title, description = item.description)
        }
    }
}

@Composable
private fun EvidenceBoard(evidence: List<EvidenceItem>) {
    ReaderCollectionPanel(title = "Evidence Board") {
        evidence.forEach { item ->
            CollectionItem(title = item.title, description = item.description)
        }
    }
}

@Composable
private fun ReaderCollectionPanel(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UiConfig.ThemeColors.ReaderChoiceCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
            .border(
                width = UiConfig.Controls.FocusThickness,
                color = UiConfig.ThemeColors.ReaderDivider,
                shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
            )
            .padding(UiConfig.Spacing.ListBuffer),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(color = UiConfig.ThemeColors.ReaderText),
        )
        content()
    }
}

@Composable
private fun CollectionItem(
    title: String,
    description: String,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(color = UiConfig.ThemeColors.ReaderText),
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = UiConfig.ThemeColors.ReaderMutedText),
            )
        }
    }
}

@Composable
private fun MapPicker(
    locations: List<MapLocation>,
    onChoice: (StoryChoice) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer)) {
        locations.forEach { location ->
            Button(
                onClick = {
                    onChoice(
                        StoryChoice(
                            text = location.title,
                            targetNodeId = location.targetNodeId,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = UiConfig.Controls.FocusThickness,
                        color = UiConfig.ThemeColors.ReaderDivider,
                        shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = UiConfig.ThemeColors.ReaderChoiceCol,
                    contentColor = UiConfig.ThemeColors.ReaderText,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
                ) {
                    Text(location.title)
                    if (location.description.isNotBlank()) {
                        Text(
                            text = location.description,
                            style = MaterialTheme.typography.bodyMedium.copy(color = UiConfig.ThemeColors.ReaderMutedText),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceButton(
    choice: StoryChoice,
    onChoice: (StoryChoice) -> Unit,
) {
    Button(
        onClick = { onChoice(choice) },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = UiConfig.Controls.FocusThickness,
                color = UiConfig.ThemeColors.ReaderDivider,
                shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = UiConfig.ThemeColors.ReaderChoiceCol,
            contentColor = UiConfig.ThemeColors.ReaderText,
        ),
    ) {
        Text(choice.text)
    }
}

@Composable
private fun ReaderError(
    message: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.alignCenterFill(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onBack) {
            Text("Back to library")
        }
    }
}

private fun Modifier.alignCenterFill(): Modifier {
    return fillMaxSize()
}
