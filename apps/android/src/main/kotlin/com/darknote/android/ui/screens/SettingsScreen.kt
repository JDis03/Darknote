package com.darknote.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darknote.android.SnippetListViewModel
import com.darknote.android.viewmodel.AuthState
import com.darknote.android.viewmodel.AuthViewModel
import com.darknote.android.viewmodel.LogType
import com.darknote.sync.engine.SyncLogType as EngineLogType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

private data class UnifiedLog(
    val timestamp: Long,
    val message: String,
    val type: EngineLogType,
    val source: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    snippetViewModel: SnippetListViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val authState by authViewModel.authState.collectAsState()
    val authUrl by authViewModel.authUrl.collectAsState()
    val authLogs by authViewModel.syncLogs.collectAsState()
    val syncLogs by snippetViewModel.syncLogs.collectAsState()
    val listState = rememberLazyListState()

    val unifiedLogs = remember(authLogs, syncLogs) {
        (authLogs.map { log ->
            val engineType = when (log.type) {
                LogType.INFO -> EngineLogType.INFO
                LogType.SUCCESS -> EngineLogType.SUCCESS
                LogType.ERROR -> EngineLogType.ERROR
                LogType.WARNING -> EngineLogType.WARNING
            }
            UnifiedLog(
                timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(log.timestamp)?.time
                    ?: System.currentTimeMillis(),
                message = log.message,
                type = engineType,
                source = "AUTH"
            )
        } + syncLogs.map { log ->
            UnifiedLog(
                timestamp = log.timestamp,
                message = log.message,
                type = log.type,
                source = "SYNC"
            )
        }).sortedBy { it.timestamp }
    }
    
    // State for manual code entry (Joplin style)
    var authCode by remember { mutableStateOf("") }

    // Auto-scroll to bottom when new logs are added
    LaunchedEffect(unifiedLogs.size) {
        if (unifiedLogs.isNotEmpty()) {
            listState.animateScrollToItem(unifiedLogs.size - 1)
        }
    }

    // Check auth status on first composition
    LaunchedEffect(Unit) {
        authViewModel.checkAuthStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Dropbox Sync Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = when (authState) {
                                is AuthState.Authenticated -> Icons.Default.CloudSync
                                else -> Icons.Default.CloudOff
                            },
                            contentDescription = null,
                            tint = when (authState) {
                                is AuthState.Authenticated -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dropbox Sync",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }

                    // Status
                    Text(
                        text = when (val state = authState) {
                            AuthState.Idle -> "Checking..."
                            AuthState.NotAuthenticated -> "Not connected"
                            AuthState.Authorizing -> "Authorizing..."
                            AuthState.Authenticated -> "Connected"
                            AuthState.Offline -> "Offline"
                            is AuthState.Error -> "Error: ${state.message}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (authState) {
                            is AuthState.Authenticated -> MaterialTheme.colorScheme.primary
                            is AuthState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // OAuth Setup Steps (Joplin style)
                    when (authState) {
                        is AuthState.NotAuthenticated, is AuthState.Error -> {
                            // Step 1: Get auth URL
                            if (authUrl == null) {
                                FilledTonalButton(
                                    onClick = { authViewModel.startAuth(context) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Get Authorization URL")
                                }
                            } else {
                                // Step 2: Show URL and code input
                                Text(
                                    text = "Step 1: Tap the URL below to authorize DarkNote:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                // Clickable auth URL
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    onClick = {
                                        authViewModel.openAuthUrl(context)
                                    }
                                ) {
                                    Text(
                                        text = authUrl!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                                
                                Text(
                                    text = "Step 2: Enter the code from Dropbox:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                // Code input field
                                OutlinedTextField(
                                    value = authCode,
                                    onValueChange = { authCode = it },
                                    label = { Text("Authorization code") },
                                    placeholder = { Text("Enter code here") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                )
                                
                                // Submit button
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { 
                                            authViewModel.logout() // Clear everything and start over
                                            authCode = ""
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reset")
                                    }
                                    
                                    FilledTonalButton(
                                        onClick = { 
                                            authViewModel.completeAuth(authCode.trim())
                                        },
                                        enabled = authCode.trim().isNotEmpty(),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Submit")
                                    }
                                }
                            }
                        }
                        is AuthState.Authorizing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Verifying authorization code...")
                            }
                        }
                        is AuthState.Authenticated -> {
                            OutlinedButton(
                                onClick = { 
                                    authViewModel.logout()
                                    authCode = "" // Clear code on logout
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Disconnect")
                            }
                        }
                        else -> {}
                    }

                    // Test buttons when authenticated
                    if (authState is AuthState.Authenticated) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { authViewModel.testUpload() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Upload")
                            }
                            
                            OutlinedButton(
                                onClick = { authViewModel.testDownload() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Download")
                            }
                        }
                    }
                }
            }

            // Sync Debug Console
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sync Console",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row {
                            TextButton(
                                onClick = {
                                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                    val text = unifiedLogs.joinToString("\n") { log ->
                                        "${sdf.format(Date(log.timestamp))} ${log.source}:${log.type.name} ${log.message}"
                                    }
                                    clipboardManager.setText(AnnotatedString(text))
                                },
                                enabled = unifiedLogs.isNotEmpty()
                            ) {
                                Text("Copy")
                            }

                            TextButton(
                                onClick = { authViewModel.clearLogs() }
                            ) {
                                Text("Clear")
                            }
                        }
                    }

                    // Logs
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(unifiedLogs) { log ->
                            SyncLogItem(log = log)
                        }
                        
                        if (unifiedLogs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No sync activity yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncLogItem(
    log: UnifiedLog,
    modifier: Modifier = Modifier
) {
    val logColor = when (log.type) {
        EngineLogType.SUCCESS -> Color(0xFF4CAF50)
        EngineLogType.ERROR -> Color(0xFFF44336)
        EngineLogType.WARNING -> Color(0xFFFF9800)
        EngineLogType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = timeStr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp)
        )
        
        Text(
            text = "${log.source}:${log.type.name}",
            style = MaterialTheme.typography.bodySmall,
            color = logColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp)
        )
        
        Text(
            text = log.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}