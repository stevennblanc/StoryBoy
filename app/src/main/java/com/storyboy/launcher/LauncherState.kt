package com.storyboy.launcher

import com.storyboy.models.LocalGamebook
import com.storyboy.models.StoreGamebook

data class LauncherState(
    val selectedTab: LauncherTab = LauncherTab.Library,
    val libraryDisplayMode: LibraryDisplayMode = LibraryDisplayMode.Book,
    val progressFilter: ProgressFilter = ProgressFilter.All,
    val selectedGenre: String? = null,
    val searchQuery: String = "",
    val selectedLocalGamebook: LocalGamebook? = null,
    val selectedStoreGamebook: StoreGamebook? = null,
    val library: List<LocalGamebook> = emptyList(),
    val store: List<StoreGamebook> = emptyList(),
    val isLoadingLibrary: Boolean = false,
    val isLoadingStore: Boolean = false,
    val message: String? = null,
)
