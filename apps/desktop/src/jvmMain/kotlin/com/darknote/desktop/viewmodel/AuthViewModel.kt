package com.darknote.desktop.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.darknote.sync.client.DropboxClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

/**
 * ViewModel for handling Dropbox authentication.
 */
class AuthViewModel(
    private val dropboxClient: DropboxClient
) {
    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    private val _authUrl = mutableStateOf<String?>(null)
    val authUrl: State<String?> = _authUrl

    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    /**
     * Check if already authorized.
     */
    fun checkAuthStatus() {
        if (dropboxClient.isAuthorized()) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    /**
     * Start OAuth flow by opening browser.
     */
    fun startAuth() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Authorizing
                val url = dropboxClient.getAuthUrl()
                _authUrl.value = url

                // Open browser automatically
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(url))
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Complete authentication with code from user.
     */
    fun completeAuth(code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Authorizing
            val result = dropboxClient.finishAuth(code.trim())

            _authState.value = if (result.isSuccess) {
                AuthState.Authenticated
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Authentication failed")
            }
        }
    }

    /**
     * Logout and clear tokens.
     */
    fun logout() {
        // TODO: Implement token clearing
        _authState.value = AuthState.NotAuthenticated
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object NotAuthenticated : AuthState()
    object Authorizing : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
