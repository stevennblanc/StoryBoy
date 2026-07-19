package com.storyboy

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.text.style.TextOverflow
import com.storyboy.core.Navigation
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig
import com.storyboy.engine.BattleConfig
import com.storyboy.engine.BattleOutcome
import com.storyboy.engine.CollectionConfig
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
            .background(ThemeManager.colors.ReaderPageCol)
            .safeDrawingPadding()
            .padding(
                horizontal = UiConfig.Spacing.ScreenPadding,
                vertical = UiConfig.Spacing.ItemGap,
            ),
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
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
    ) {
        ReaderTopBar(
            title = gamebook.metadata.title,
            evidenceConfig = gamebook.evidenceConfig,
            inventoryConfig = gamebook.inventoryConfig,
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
            verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
        ) {
            if (showEvidence && gamebook.evidenceConfig.enabled) {
                EvidenceBoard(
                    title = gamebook.evidenceConfig.label,
                    evidence = state.collectedEvidence,
                )
            }

            if (showInventory && gamebook.inventoryConfig.enabled) {
                InventoryPanel(
                    title = gamebook.inventoryConfig.label,
                    inventory = state.collectedInventory,
                )
            }

            state.currentNodeImages.forEach { image ->
                StoryInlineImage(image = image)
            }

            Text(
                text = node.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = ThemeManager.colors.ReaderText,
                    fontFamily = UiConfig.Fonts.SecondaryFontFamily,
                    lineHeight = UiConfig.Fonts.ReaderLineHeight * ThemeManager.fontScale,
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
                    style = MaterialTheme.typography.headlineMedium.copy(color = ThemeManager.colors.ReaderText),
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
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
            if (image.caption.isNotBlank()) {
                Text(
                    text = image.caption,
                    style = MaterialTheme.typography.bodyMedium.copy(color = ThemeManager.colors.ReaderMutedText),
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
                    containerColor = ThemeManager.colors.ReaderChoiceCol,
                    contentColor = ThemeManager.colors.ReaderText,
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
            style = MaterialTheme.typography.bodyMedium.copy(color = ThemeManager.colors.ReaderMutedText),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(color = ThemeManager.colors.ReaderText),
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
            style = MaterialTheme.typography.headlineMedium.copy(color = ThemeManager.colors.ReaderText),
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
                containerColor = ThemeManager.colors.ReaderChoiceCol,
                contentColor = ThemeManager.colors.ReaderText,
            ),
        ) {
            Text("Submit")
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    evidenceConfig: CollectionConfig,
    inventoryConfig: CollectionConfig,
    evidenceCount: Int,
    inventoryCount: Int,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onEvidence: () -> Unit,
    onInventory: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("Library", maxLines = 1)
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(color = ThemeManager.colors.ReaderMutedText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onRestart) {
                Text("Restart", maxLines = 1)
            }
        }
        if (inventoryConfig.enabled || evidenceConfig.enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (inventoryConfig.enabled) {
                    TextButton(onClick = onInventory, enabled = inventoryCount > 0) {
                        Text(inventoryConfig.buttonLabel(inventoryCount), maxLines = 1)
                    }
                }
                if (evidenceConfig.enabled) {
                    TextButton(onClick = onEvidence, enabled = evidenceCount > 0) {
                        Text(evidenceConfig.buttonLabel(evidenceCount), maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun CollectionConfig.buttonLabel(count: Int): String {
    return if (showCount) "$label $count" else label
}

@Composable
private fun InventoryPanel(
    title: String,
    inventory: List<InventoryItem>,
) {
    var expandedId by remember { mutableStateOf<String?>(null) }
    ReaderCollectionPanel(title = title) {
        inventory.forEach { item ->
            ExpandableCollectionItem(
                title = item.title,
                description = item.description,
                detail = item.detail,
                imagePath = item.image,
                expanded = expandedId == item.id,
                onToggle = { expandedId = if (expandedId == item.id) null else item.id },
            )
        }
    }
}

@Composable
private fun EvidenceBoard(
    title: String,
    evidence: List<EvidenceItem>,
) {
    var expandedId by remember { mutableStateOf<String?>(null) }
    ReaderCollectionPanel(title = title) {
        evidence.forEach { item ->
            ExpandableCollectionItem(
                title = item.title,
                description = item.description,
                detail = item.detail,
                imagePath = item.image,
                expanded = expandedId == item.id,
                onToggle = { expandedId = if (expandedId == item.id) null else item.id },
            )
        }
    }
}

@Composable
private fun ExpandableCollectionItem(
    title: String,
    description: String,
    detail: String,
    imagePath: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val hasMore = detail.isNotBlank() || imagePath != null

    Column(
        modifier = if (hasMore) Modifier.fillMaxWidth().clickable(onClick = onToggle) else Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge.copy(color = ThemeManager.colors.ReaderText),
            )
            if (hasMore) {
                Text(
                    text = if (expanded) "Close" else "View",
                    style = MaterialTheme.typography.labelLarge.copy(color = ThemeManager.colors.ReaderMutedText),
                )
            }
        }
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = ThemeManager.colors.ReaderMutedText),
            )
        }
        if (expanded && hasMore) {
            if (imagePath != null) {
                StoryInlineImage(image = StoryImage(path = imagePath, caption = ""))
            }
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium.copy(color = ThemeManager.colors.ReaderText),
                )
            }
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
            .background(ThemeManager.colors.ReaderChoiceCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
            .border(
                width = UiConfig.Controls.FocusThickness,
                color = ThemeManager.colors.ReaderDivider,
                shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
            )
            .padding(UiConfig.Spacing.ListBuffer),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(color = ThemeManager.colors.ReaderText),
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
            style = MaterialTheme.typography.labelLarge.copy(color = ThemeManager.colors.ReaderText),
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = ThemeManager.colors.ReaderMutedText),
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeManager.colors.ReaderChoiceCol,
                    contentColor = ThemeManager.colors.ReaderText,
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
                            style = MaterialTheme.typography.bodyMedium.copy(color = ThemeManager.colors.ReaderMutedText),
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
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = ThemeManager.colors.ReaderChoiceCol,
            contentColor = ThemeManager.colors.ReaderText,
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
