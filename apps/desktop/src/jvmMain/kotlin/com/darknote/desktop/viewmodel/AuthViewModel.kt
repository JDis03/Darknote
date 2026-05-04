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

    private val _syncLogs = mutableStateOf<List<SyncLog>>(emptyList())
    val syncLogs: State<List<SyncLog>> = _syncLogs

    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    private fun addLog(message: String, type: LogType = LogType.INFO) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
        val log = SyncLog(
            timestamp = timestamp,
            message = message,
            type = type
        )
        _syncLogs.value = (_syncLogs.value + log).takeLast(50) // Keep last 50 logs
    }

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
     * Test upload - uploads a test file to verify sync works.
     */
    fun testUpload() {
        viewModelScope.launch {
            try {
                addLog("Starting test upload...", LogType.INFO)
                val timestamp = System.currentTimeMillis()
                val testContent = "Test snippet content created at $timestamp"
                
                // Create temp file
                val tempFile = java.io.File.createTempFile("test-snippet-$timestamp", ".txt")
                tempFile.writeText(testContent)
                
                val remotePath = "/darknote/test-snippet-$timestamp.txt"
                addLog("Uploading to $remotePath", LogType.INFO)
                
                val result = dropboxClient.uploadFile(tempFile.absolutePath, remotePath)
                
                if (result.isSuccess) {
                    addLog("Upload successful! Rev: ${result.getOrNull()}", LogType.SUCCESS)
                } else {
                    addLog("Upload failed: ${result.exceptionOrNull()?.message}", LogType.ERROR)
                }
                
                // Cleanup temp file
                tempFile.delete()
            } catch (e: Exception) {
                addLog("Upload exception: ${e.message}", LogType.ERROR)
                e.printStackTrace()
            }
        }
    }

    /**
     * Test download - lists files in Dropbox to verify sync works.
     */
    fun testDownload() {
        viewModelScope.launch {
            try {
                addLog("Listing files in /darknote...", LogType.INFO)
                
                val result = dropboxClient.listFiles("/darknote")
                
                if (result.isSuccess) {
                    val files = result.getOrNull() ?: emptyList()
                    addLog("Found ${files.size} files in Dropbox", LogType.SUCCESS)
                    files.forEach { file ->
                        addLog("  ${file.name} (${file.size} bytes)", LogType.INFO)
                    }
                } else {
                    addLog("List files failed: ${result.exceptionOrNull()?.message}", LogType.ERROR)
                }
            } catch (e: Exception) {
                addLog("List exception: ${e.message}", LogType.ERROR)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Clear sync logs.
     */
    fun clearLogs() {
        _syncLogs.value = emptyList()
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

data class SyncLog(
    val timestamp: String,
    val message: String,
    val type: LogType
)

enum class LogType {
    INFO,
    SUCCESS,
    ERROR,
    WARNING
}
