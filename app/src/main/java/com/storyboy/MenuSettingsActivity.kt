package com.storyboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.storyboy.account.AccountState
import com.storyboy.account.AccountViewModel
import com.storyboy.core.AppearanceMode
import com.storyboy.core.AppearanceSettingsRepository
import com.storyboy.core.MotionMode
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig
import com.storyboy.core.rememberAppearanceSettings
import com.storyboy.updater.UpdateStatus
import com.storyboy.updater.UpdateViewModel
import kotlin.math.roundToInt

class MenuSettingsActivity : ComponentActivity() {
    private val updateViewModel: UpdateViewModel by viewModels()
    private val accountViewModel: AccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ThemeManager.StoryBoyTheme {
                val updateStatus by updateViewModel.status.collectAsState()
                val accountState by accountViewModel.state.collectAsState()
                SettingsScreen(
                    updateStatus = updateStatus,
                    accountState = accountState,
                    accountViewModel = accountViewModel,
                    onBack = ::finish,
                    onCheckUpdate = updateViewModel::checkForUpdates,
                    onDownloadUpdate = updateViewModel::downloadAndInstall,
                    onInstallUpdate = updateViewModel::installPendingUpdate,
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    updateStatus: UpdateStatus,
    accountState: AccountState,
    accountViewModel: AccountViewModel,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val appearanceRepository = remember(context) { AppearanceSettingsRepository(context) }
    val settings by rememberAppearanceSettings()
    val colors = ThemeManager.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.BackgroundCol)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
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
                Text(text = "Account, appearance, updates", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onBack) {
                Text("Library")
            }
        }

        AccountPanel(state = accountState, viewModel = accountViewModel)

        SettingPanel(title = "Appearance") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
                ) {
                    Text(text = "Dark mode", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (settings.mode == AppearanceMode.Dark) "Dark interface and reader" else "Light interface and reader",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = settings.mode == AppearanceMode.Dark,
                    onCheckedChange = { enabled ->
                        appearanceRepository.setMode(if (enabled) AppearanceMode.Dark else AppearanceMode.Light)
                    },
                )
            }

            HorizontalDivider(color = colors.SubDivider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
                    Text(text = "Font size", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${(settings.fontScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = { appearanceRepository.setFontScale(AppearanceSettingsRepository.DefaultFontScale) },
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
                onValueChange = appearanceRepository::setFontScale,
                valueRange = AppearanceSettingsRepository.MinFontScale..AppearanceSettingsRepository.MaxFontScale,
                steps = 9,
            )

            HorizontalDivider(color = colors.SubDivider)

            Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
                Text(text = "Motion", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = motionModeDescription(settings.motionMode),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
                ) {
                    MotionModeButton(
                        label = "Full",
                        selected = settings.motionMode == MotionMode.Full,
                        onClick = { appearanceRepository.setMotionMode(MotionMode.Full) },
                        modifier = Modifier.weight(1f),
                    )
                    MotionModeButton(
                        label = "Reduced",
                        selected = settings.motionMode == MotionMode.Reduced,
                        onClick = { appearanceRepository.setMotionMode(MotionMode.Reduced) },
                        modifier = Modifier.weight(1f),
                    )
                }
                MotionModeButton(
                    label = "None",
                    selected = settings.motionMode == MotionMode.None,
                    onClick = { appearanceRepository.setMotionMode(MotionMode.None) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SettingPanel(title = "App updates") {
            UpdateControls(
                status = updateStatus,
                onCheck = onCheckUpdate,
                onDownload = onDownloadUpdate,
                onInstall = onInstallUpdate,
            )
        }

        SettingPanel(title = "Preview") {
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

@Composable
private fun AccountPanel(
    state: AccountState,
    viewModel: AccountViewModel,
) {
    val session = state.session
    if (session != null) {
        var displayName by remember(session.userId) { mutableStateOf(session.displayName) }
        SettingPanel(title = "Account") {
            Text(text = session.email, style = MaterialTheme.typography.bodyLarge)
            state.ownedBookCount?.let { count ->
                Text(
                    text = "$count book${if (count == 1) "" else "s"} in your StoryBoy library",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Display name") },
            )
            Button(
                onClick = { viewModel.saveDisplayName(displayName) },
                enabled = !state.isBusy && displayName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save profile")
            }
            if (state.message.isNotBlank()) {
                Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(
                onClick = viewModel::signOut,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign out")
            }
        }
    } else {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var displayName by remember { mutableStateOf("") }
        SettingPanel(title = "Account") {
            Text(
                text = "Sign in to keep your StoryBoy library and profile across devices.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Email") },
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Display name (new accounts)") },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
            ) {
                Button(
                    onClick = { viewModel.signIn(email, password) },
                    enabled = !state.isBusy && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Sign in")
                }
                Button(
                    onClick = { viewModel.signUp(email, password, displayName) },
                    enabled = !state.isBusy && email.isNotBlank() && password.length >= 8,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Create account")
                }
            }
            if (state.message.isNotBlank()) {
                Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MotionModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ThemeManager.colors
    val buttonColors = if (selected) {
        ButtonDefaults.buttonColors(
            containerColor = colors.AccentCol,
            contentColor = colors.BackgroundCol,
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = colors.ReaderChoiceCol,
            contentColor = colors.ReaderText,
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = buttonColors,
    ) {
        Text(label)
    }
}

private fun motionModeDescription(motionMode: MotionMode): String {
    return when (motionMode) {
        MotionMode.Full -> "Android can use short transitions, highlights, and rolls when useful."
        MotionMode.Reduced -> "Android keeps feedback mostly static with minimal motion."
        MotionMode.None -> "E-ink style: direct redraws and persistent feedback only."
    }
}

@Composable
private fun UpdateControls(
    status: UpdateStatus,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    when (status) {
        UpdateStatus.Idle -> Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) {
            Text("Check for app updates")
        }

        UpdateStatus.Checking -> Text(text = "Checking for app updates", style = MaterialTheme.typography.bodyMedium)
        UpdateStatus.Downloading -> Text(text = "Downloading app update", style = MaterialTheme.typography.bodyMedium)
        UpdateStatus.UpToDate -> Text(text = "StoryBoy is up to date", style = MaterialTheme.typography.bodyMedium)
        UpdateStatus.ReadyToInstall -> Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
            Text("Install app update")
        }

        is UpdateStatus.Available -> Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
            Text("Install StoryBoy ${status.manifest.versionName}")
        }

        is UpdateStatus.Failed -> Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
            Text(text = status.message, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onCheck) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun SettingPanel(
    title: String,
    content: @Composable () -> Unit,
) {
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
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        content()
    }
}
