package com.storyboy.updater

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val updater = AppUpdater(application)
    private var pendingManifest: UpdateManifest? = null
    private var pendingApk: File? = null

    private val mutableStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = mutableStatus.asStateFlow()

    fun checkForUpdates() {
        mutableStatus.value = UpdateStatus.Checking
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    updater.checkForUpdate()
                }
            }.onSuccess { manifest ->
                pendingManifest = manifest
                mutableStatus.value = if (manifest == null) {
                    UpdateStatus.UpToDate
                } else {
                    UpdateStatus.Available(manifest)
                }
            }.onFailure { throwable ->
                mutableStatus.value = UpdateStatus.Failed(throwable.message ?: "Update check failed")
            }
        }
    }

    fun downloadAndInstall() {
        val manifest = pendingManifest ?: return
        mutableStatus.value = UpdateStatus.Downloading
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    updater.downloadUpdate(manifest)
                }
            }.onSuccess { apk ->
                pendingApk = apk
                mutableStatus.value = UpdateStatus.ReadyToInstall
                updater.installUpdate(apk)
            }.onFailure { throwable ->
                mutableStatus.value = UpdateStatus.Failed(throwable.message ?: "Update download failed")
            }
        }
    }

    fun installPendingUpdate() {
        pendingApk?.let(updater::installUpdate)
    }
}
