package com.storyboy.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyboy.core.ThemeManager
import com.storyboy.core.UiConfig
import com.storyboy.models.CatalogueBook
import com.storyboy.models.LocalGamebook
import com.storyboy.models.StoreGamebook

@Composable
internal fun StoreBookDetailPage(
    gamebook: StoreGamebook,
    catalogue: CatalogueBook?,
    owned: Boolean,
    isSignedIn: Boolean,
    onClose: () -> Unit,
    onDownload: () -> Unit,
    onAcquire: () -> Unit,
    onRead: (() -> Unit)?,
) {
    BookDetailScaffold(backLabel = "Store", onClose = onClose) {
        BookDetailHero(
            posterPath = gamebook.posterPath,
            title = gamebook.metadata.title,
            author = catalogue?.author ?: gamebook.metadata.author,
            genre = catalogue?.genre ?: gamebook.metadata.genre,
            badge = if (owned) "In your library" else catalogue?.priceLabel() ?: "Free",
        )

        Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
            if (onRead != null) {
                Button(onClick = onRead, modifier = Modifier.fillMaxWidth()) {
                    Text("Read")
                }
            }
            Button(
                onClick = onDownload,
                enabled = !gamebook.isDownloaded || gamebook.updateAvailable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(gamebook.detailActionText())
            }
            if (isSignedIn && !owned) {
                TextButton(onClick = onAcquire, modifier = Modifier.fillMaxWidth()) {
                    Text("Add to StoryBoy library — ${catalogue?.priceLabel() ?: "Free"}")
                }
            } else if (!isSignedIn) {
                Text(
                    text = "Sign in from Settings to keep your library across devices.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Text(
            text = catalogue?.description?.ifBlank { null } ?: gamebook.metadata.description,
            style = MaterialTheme.typography.bodyLarge,
        )

        FeatureChips(
            features = catalogue?.features?.ifEmpty { null } ?: gamebook.metadata.featureLabels(),
        )

        catalogue?.let { book ->
            BookStatsPanel(book = book, installedVersion = gamebook.localVersion)
            if (book.about.isNotBlank()) {
                DetailSectionPanel(title = "About this book") {
                    Text(text = book.about, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (catalogue == null && gamebook.isDownloaded) {
            Text(
                text = "Installed: ${gamebook.localVersion}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun LocalBookDetailPage(
    gamebook: LocalGamebook,
    catalogue: CatalogueBook?,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
) {
    BookDetailScaffold(backLabel = "Library", onClose = onClose) {
        BookDetailHero(
            posterPath = gamebook.posterPath,
            title = gamebook.metadata.title,
            author = gamebook.metadata.author,
            genre = gamebook.metadata.genre,
            badge = if (gamebook.hasPlaythroughInProgress) "In progress" else "Ready to read",
        )

        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
            Text(if (gamebook.hasPlaythroughInProgress) "Resume" else "Play")
        }

        Text(text = gamebook.metadata.description, style = MaterialTheme.typography.bodyLarge)

        FeatureChips(
            features = catalogue?.features?.ifEmpty { null } ?: gamebook.metadata.featureLabels(),
        )

        catalogue?.let { book ->
            BookStatsPanel(book = book, installedVersion = gamebook.metadata.version)
            if (book.about.isNotBlank()) {
                DetailSectionPanel(title = "About this book") {
                    Text(text = book.about, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text("Delete from device")
        }
    }
}

@Composable
private fun BookDetailScaffold(
    backLabel: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onClose) {
                Text("< $backLabel")
            }
        }
        content()
    }
}

@Composable
private fun BookDetailHero(
    posterPath: String?,
    title: String,
    author: String,
    genre: String,
    badge: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.SectionGap),
        verticalAlignment = Alignment.Top,
    ) {
        ArtworkFrame(
            artworkPath = posterPath,
            displayMode = LibraryDisplayMode.Book,
            modifier = Modifier.width(132.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap)) {
            Text(text = title, style = MaterialTheme.typography.displayMedium)
            Text(text = author, style = MaterialTheme.typography.labelLarge)
            if (genre.isNotBlank()) {
                Text(text = genre, style = MaterialTheme.typography.bodyMedium)
            }
            DetailBadge(text = badge)
        }
    }
}

@Composable
private fun DetailBadge(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(ThemeManager.colors.SurfaceCol, RoundedCornerShape(UiConfig.Controls.ButtonRadius))
            .border(
                width = UiConfig.Controls.FocusThickness,
                color = ThemeManager.colors.AccentCol,
                shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
            )
            .padding(horizontal = UiConfig.Spacing.ListBuffer, vertical = UiConfig.Spacing.ItemGap),
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BookStatsPanel(
    book: CatalogueBook,
    installedVersion: String?,
) {
    val stats = buildList {
        book.nodeCount?.let { add("Length" to "$it passages") }
        book.endingCount?.let { add("Endings" to "$it") }
        if (book.language.isNotBlank()) add("Language" to book.language)
        book.fileSizeLabel()?.let { add("Size" to it) }
        if (book.version.isNotBlank()) add("Version" to book.version)
        if (book.publishedOn.isNotBlank()) add("Published" to book.publishedOn)
        if (book.publisher.isNotBlank()) add("Publisher" to book.publisher)
        installedVersion?.let { add("Installed" to it) }
    }
    if (stats.isEmpty()) return

    DetailSectionPanel(title = "Book details") {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
            verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.ItemGap),
        ) {
            stats.forEach { (label, value) ->
                Column(
                    modifier = Modifier
                        .border(
                            width = UiConfig.Controls.FocusThickness,
                            color = ThemeManager.colors.SubDivider,
                            shape = RoundedCornerShape(UiConfig.Controls.ButtonRadius),
                        )
                        .padding(UiConfig.Spacing.ListBuffer),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    Text(text = value, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DetailSectionPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
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

private fun StoreGamebook.detailActionText(): String {
    return when {
        updateAvailable -> "Update downloaded copy"
        isDownloaded -> "Downloaded"
        else -> "Download for offline play"
    }
}
