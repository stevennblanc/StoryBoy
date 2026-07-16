package com.storyboy.updater

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object Downloading : UpdateStatus
    data object ReadyToInstall : UpdateStatus
    data object UpToDate : UpdateStatus
    data class Available(val manifest: UpdateManifest) : UpdateStatus
    data class Failed(val message: String) : UpdateStatus
}
