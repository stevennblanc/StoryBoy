package com.storyboy

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
import com.storyboy.core.Navigation
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig
import com.storyboy.engine.StoryChoice
import com.storyboy.engine.StoryEngineState
import com.storyboy.engine.StoryEngineViewModel

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
) {
    val gamebook = state.gamebook ?: return
    val node = state.currentNode ?: return

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
    ) {
        ReaderTopBar(
            title = gamebook.metadata.title,
            onBack = onBack,
            onRestart = onRestart,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.SectionGap),
        ) {
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
    onBack: () -> Unit,
    onRestart: () -> Unit,
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
        TextButton(onClick = onRestart) {
            Text("Restart")
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
