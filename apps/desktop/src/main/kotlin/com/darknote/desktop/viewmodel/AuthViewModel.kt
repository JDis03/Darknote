package com.darknote.desktop.viewmodel

import com.darknote.sync.client.DropboxClient
import com.darknote.sync.client.JvmDropboxClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

/**
 * Authentication states for the Dropbox OAuth flow.
 */
sealed class AuthState {
    data object Idle : AuthState()
    data object NotAuthenticated : AuthState()
    data object Authorizing : AuthState()
    data object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel for the Dropbox OAuth flow on Desktop.
 *
 * Flow: startAuth() opens the browser to Dropbox's consent page ->
 * user approves and copies a code -> completeAuth(code) exchanges it
 * for tokens (persisted by [JvmDropboxClient] via java.util.prefs).
 */
class AuthViewModel(
    private val dropboxClient: DropboxClient
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _authUrl = MutableStateFlow<String?>(null)
    val authUrl: StateFlow<String?> = _authUrl.asStateFlow()

    init {
        checkAuthStatus()
    }

    /** Refresh [authState] based on the current stored credentials. */
    fun checkAuthStatus() {
        _authState.value = if (dropboxClient.isAuthorized()) {
            AuthState.Authenticated
        } else {
            AuthState.NotAuthenticated
        }
    }

    /**
     * Starts the OAuth flow: generates the consent URL, tries to open it in
     * the system browser, and exposes it via [authUrl] so the UI can show a
     * "copy link" fallback if the browser couldn't be opened automatically.
     */
    fun startAuth() {
        scope.launch {
            try {
                _authState.value = AuthState.Authorizing
                val url = dropboxClient.getAuthUrl()
                _authUrl.value = url
                openInBrowser(url)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to start authentication")
            }
        }
    }

    /** Completes the OAuth flow using the code the user pasted from the browser. */
    fun completeAuth(code: String) {
        if (code.isBlank()) return
        scope.launch {
            _authState.value = AuthState.Authorizing
            val result = dropboxClient.finishAuth(code.trim())
            _authState.value = if (result.isSuccess) {
                _authUrl.value = null
                AuthState.Authenticated
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Authentication failed")
            }
        }
    }

    /** Clears stored credentials and returns to the disconnected state. */
    fun logout() {
        (dropboxClient as? JvmDropboxClient)?.logout()
        _authUrl.value = null
        _authState.value = AuthState.NotAuthenticated
    }

    /** Dismisses an [AuthState.Error] back to [AuthState.NotAuthenticated]. */
    fun dismissError() {
        checkAuthStatus()
    }

    private fun openInBrowser(url: String) {
        runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                return
            }
        }
        // Fallback for Linux desktops where java.awt.Desktop isn't wired up
        // (common on some window managers/KDE Wayland setups).
        runCatching { ProcessBuilder("xdg-open", url).start() }
    }
}
