package com.storyboy.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.storyboy.models.LocalGamebook
import com.storyboy.models.StoreGamebook
import com.storyboy.repository.GamebookLibraryRepository
import com.storyboy.repository.GamebookStoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val libraryRepository = GamebookLibraryRepository(application)
    private val storeRepository = GamebookStoreRepository(application)
    private val mutableState = MutableStateFlow(LauncherState())

    val state: StateFlow<LauncherState> = mutableState.asStateFlow()

    init {
        refreshLibrary()
    }

    fun selectTab(tab: LauncherTab) {
        mutableState.update {
            it.copy(
                selectedTab = tab,
                selectedLocalGamebook = null,
                selectedStoreGamebook = null,
                message = null,
            )
        }
        if (tab == LauncherTab.Store && mutableState.value.store.isEmpty()) {
            refreshStore()
        }
    }

    fun selectLibraryDisplayMode(displayMode: LibraryDisplayMode) {
        mutableState.update { it.copy(libraryDisplayMode = displayMode) }
    }

    fun selectLibrarySortMode(sortMode: LibrarySortMode) {
        mutableState.update { it.copy(librarySortMode = sortMode) }
    }

    fun selectProgressFilter(filter: ProgressFilter) {
        mutableState.update { it.copy(progressFilter = filter) }
    }

    fun selectGenreFilter(genre: String?) {
        mutableState.update { it.copy(selectedGenre = genre) }
    }

    fun updateSearchQuery(query: String) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun selectLocalGamebook(gamebook: LocalGamebook) {
        mutableState.update { it.copy(selectedLocalGamebook = gamebook) }
    }

    fun selectStoreGamebook(gamebook: StoreGamebook) {
        mutableState.update { it.copy(selectedStoreGamebook = gamebook) }
    }

    fun closeDetail() {
        mutableState.update {
            it.copy(
                selectedLocalGamebook = null,
                selectedStoreGamebook = null,
            )
        }
    }

    fun refreshLibrary() {
        mutableState.update { it.copy(isLoadingLibrary = true, message = null) }
        viewModelScope.launch {
            val library = withContext(Dispatchers.IO) {
                libraryRepository.listLocalGamebooks()
            }
            mutableState.update {
                it.copy(library = library, isLoadingLibrary = false)
            }
        }
    }

    fun refreshStore() {
        mutableState.update { it.copy(isLoadingStore = true, message = null) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    storeRepository.listStoreGamebooks()
                }
            }.onSuccess { store ->
                mutableState.update {
                    it.copy(store = store, isLoadingStore = false)
                }
            }.onFailure { throwable ->
                mutableState.update {
                    it.copy(
                        isLoadingStore = false,
                        message = throwable.message ?: "Store is unavailable",
                    )
                }
            }
        }
    }

    fun download(storeGamebook: StoreGamebook) {
        mutableState.update { it.copy(isLoadingStore = true, message = null) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    storeRepository.download(storeGamebook)
                }
            }.onSuccess {
                refreshLibrary()
                refreshStore()
                mutableState.update { it.copy(selectedStoreGamebook = null) }
            }.onFailure { throwable ->
                mutableState.update {
                    it.copy(
                        isLoadingStore = false,
                        message = throwable.message ?: "Download failed",
                    )
                }
            }
        }
    }

    fun deleteLocalGamebook(localGamebook: LocalGamebook) {
        mutableState.update { it.copy(isLoadingLibrary = true, message = null) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                libraryRepository.deleteLocalGamebook(localGamebook)
            }
            mutableState.update {
                it.copy(
                    selectedLocalGamebook = null,
                    isLoadingLibrary = false,
                    message = "${localGamebook.metadata.title} was removed from this device.",
                )
            }
            refreshLibrary()
            if (mutableState.value.store.isNotEmpty()) {
                refreshStore()
            }
        }
    }

    fun markStarted(localGamebook: LocalGamebook) {
        libraryRepository.markPlaythroughStarted(localGamebook.metadata.id)
        refreshLibrary()
    }
}
