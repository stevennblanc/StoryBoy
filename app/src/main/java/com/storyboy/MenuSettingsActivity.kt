package com.storyboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.storyboy.core.AppearanceMode
import com.storyboy.core.AppearanceSettingsRepository
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig
import com.storyboy.core.rememberAppearanceSettings
import kotlin.math.roundToInt

class MenuSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ThemeManager.StoryBoyTheme {
                SettingsScreen(onBack = ::finish)
            }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { AppearanceSettingsRepository(context) }
    val settings by rememberAppearanceSettings()
    val colors = ThemeManager.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.BackgroundCol)
            .safeDrawingPadding()
            .padding(UiConfig.Spacing.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.SectionGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
                Text(text = "Settings", style = MaterialTheme.typography.displayMedium)
                Text(text = "Appearance", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onBack) {
                Text("Library")
            }
        }

        SettingPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
                ) {
                    Text(text = "Dark mode", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = if (settings.mode == AppearanceMode.Dark) "Dark interface and reader" else "Light interface and reader",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = settings.mode == AppearanceMode.Dark,
                    onCheckedChange = { enabled ->
                        repository.setMode(if (enabled) AppearanceMode.Dark else AppearanceMode.Light)
                    },
                )
            }
        }

        SettingPanel {
            Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
                        Text(text = "Font size", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "${(settings.fontScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Button(
                        onClick = { repository.setFontScale(AppearanceSettingsRepository.DefaultFontScale) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.ReaderChoiceCol,
                            contentColor = colors.ReaderText,
                        ),
                    ) {
                        Text("Reset")
                    }
                }

                Slider(
                    value = settings.fontScale,
                    onValueChange = repository::setFontScale,
                    valueRange = AppearanceSettingsRepository.MinFontScale..AppearanceSettingsRepository.MaxFontScale,
                    steps = 9,
                )
            }
        }

        HorizontalDivider(color = colors.SubDivider)

        SettingPanel {
            Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer)) {
                Text(text = "Preview", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Nathan turned the page and listened to the rain against the office window.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = colors.ReaderChoiceCol,
                        disabledContentColor = colors.ReaderText,
                    ),
                ) {
                    Text("Choice button preview")
                }
            }
        }
    }
}

@Composable
private fun SettingPanel(content: @Composable () -> Unit) {
    val colors = ThemeManager.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.ElevatedSurfaceCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
            .border(
                width = UiConfig.Controls.FocusThickness,
                color = colors.SubDivider,
                shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
            )
            .padding(UiConfig.Spacing.ListBuffer),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ListBuffer),
    ) {
        content()
    }
}
