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
                
                println("Auth URL: $url")

                // Try multiple methods to open browser
                var opened = false
                
                // Method 1: Java Desktop API
                if (Desktop.isDesktopSupported()) {
                    val desktop = Desktop.getDesktop()
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        try {
                            desktop.browse(URI(url))
                            opened = true
                            println("Browser opened successfully via Desktop API")
                        } catch (e: Exception) {
                            println("Failed to open via Desktop API: ${e.message}")
                        }
                    }
                }
                
                // Method 2: xdg-open (Linux)
                if (!opened) {
                    try {
                        ProcessBuilder("xdg-open", url).start()
                        opened = true
                        println("Browser opened successfully via xdg-open")
                    } catch (e: Exception) {
                        println("Failed to open via xdg-open: ${e.message}")
                    }
                }
                
                // Method 3: gnome-open (GNOME)
                if (!opened) {
                    try {
                        ProcessBuilder("gnome-open", url).start()
                        opened = true
                        println("Browser opened successfully via gnome-open")
                    } catch (e: Exception) {
                        println("Failed to open via gnome-open: ${e.message}")
                    }
                }
                
                if (!opened) {
                    println("WARNING: Could not open browser automatically. Please copy the URL manually.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
     * Skip authentication and continue in offline mode.
     */
    fun skipAuth() {
        _authState.value = AuthState.Offline
    }

    /**
     * Logout and clear tokens.
     */
    fun logout() {
        // TODO: Implement token clearing
        _authState.value = AuthState.NotAuthenticated
    }

    /**
     * Authentication states.
     */
}

sealed class AuthState {
    object Idle : AuthState()
    object NotAuthenticated : AuthState()
    object Authorizing : AuthState()
    object Authenticated : AuthState()
    object Offline : AuthState()  // Continue without Dropbox
    data class Error(val message: String) : AuthState()
}
