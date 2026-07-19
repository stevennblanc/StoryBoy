package com.storyboy.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.storyboy.data.AuthSession
import com.storyboy.data.SupabaseAuthRepository
import com.storyboy.repository.StoreCatalogueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AccountState(
    val isBusy: Boolean = false,
    val session: AuthSession? = null,
    val ownedBookCount: Int? = null,
    val message: String = "",
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = SupabaseAuthRepository(application)
    private val catalogueRepository = StoreCatalogueRepository()
    private val mutableState = MutableStateFlow(AccountState())

    val state: StateFlow<AccountState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        mutableState.update { it.copy(session = authRepository.currentSession()) }
        val session = mutableState.value.session ?: return
        viewModelScope.launch {
            val ownedCount = runCatching {
                withContext(Dispatchers.IO) {
                    val token = authRepository.validAccessToken() ?: return@withContext null
                    catalogueRepository.fetchOwnedBookIds(token).size
                }
            }.getOrNull()
            mutableState.update {
                if (it.session?.userId == session.userId) it.copy(ownedBookCount = ownedCount) else it
            }
        }
    }

    fun signIn(email: String, password: String) {
        runAccountAction("Signing in...") {
            authRepository.signIn(email.trim(), password)
            ""
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        runAccountAction("Creating account...") {
            val session = authRepository.signUp(email.trim(), password, displayName)
            if (session == null) {
                "Account created. Check your email for a confirmation link, then sign in."
            } else {
                ""
            }
        }
    }

    fun saveDisplayName(displayName: String) {
        runAccountAction("Saving...") {
            authRepository.updateDisplayName(displayName)
            "Profile saved."
        }
    }

    fun signOut() {
        authRepository.signOut()
        mutableState.value = AccountState()
    }

    private fun runAccountAction(busyMessage: String, action: suspend () -> String) {
        mutableState.update { it.copy(isBusy = true, message = busyMessage) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { action() }
            }.onSuccess { message ->
                mutableState.update {
                    it.copy(
                        isBusy = false,
                        message = message,
                        session = authRepository.currentSession(),
                    )
                }
                refresh()
            }.onFailure { throwable ->
                mutableState.update {
                    it.copy(
                        isBusy = false,
                        message = throwable.message ?: "Something went wrong.",
                    )
                }
            }
        }
    }
}
