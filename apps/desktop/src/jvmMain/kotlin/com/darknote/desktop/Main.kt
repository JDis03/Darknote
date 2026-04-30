package com.darknote.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.darknote.desktop.clipboard.DesktopClipboardManager
import com.darknote.core.clipboard.ClipboardSanitizer
import com.darknote.core.model.ClipboardSettings
import com.darknote.desktop.ui.tree.*
import com.darknote.desktop.viewmodel.AuthState
import com.darknote.desktop.viewmodel.AuthViewModel
import com.darknote.desktop.viewmodel.SnippetTreeViewModel
import com.darknote.sync.client.DropboxClientFactory
import kotlinx.coroutines.launch

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DarkNote - Snippet Manager"
    ) {
        MaterialTheme {
            AppWithAuth()
        }
    }
}

@Composable
fun AppWithAuth() {
    // Crear cliente Dropbox
    val dropboxClient = remember { DropboxClientFactory.create() }
    val authViewModel = remember { AuthViewModel(dropboxClient) }

    // Verificar estado inicial
    LaunchedEffect(Unit) {
        authViewModel.checkAuthStatus()
    }

    val authState by authViewModel.authState

    when (authState) {
        is AuthState.Authenticated -> MainScreen()
        is AuthState.NotAuthenticated,
        is AuthState.Idle -> AuthScreen(authViewModel)
        is AuthState.Authorizing -> LoadingScreen("Authenticating with Dropbox...")
        is AuthState.Error -> AuthScreen(authViewModel, (authState as AuthState.Error).message)
    }
}

@Composable
fun AuthScreen(viewModel: AuthViewModel, errorMessage: String? = null) {
    var authCode by remember { mutableStateOf("") }
    val authUrl by viewModel.authUrl
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(0.5f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Cloud",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    "Welcome to DarkNote",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    "Connect with Dropbox to sync your snippets across devices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (authUrl == null) {
                    Button(
                        onClick = { viewModel.startAuth() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect with Dropbox")
                    }
                } else {
                    Text(
                        "Browser opened! Enter the code from Dropbox:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = authCode,
                        onValueChange = { authCode = it },
                        label = { Text("Authorization Code") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { viewModel.completeAuth(authCode) },
                        enabled = authCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Complete Authentication")
                    }

                    TextButton(
                        onClick = { viewModel.startAuth() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Browser Again")
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun MainScreen() {
    val viewModel = remember { SnippetTreeViewModel.create() }
    val clipboardManager = remember {
        DesktopClipboardManager(ClipboardSanitizer(ClipboardSettings.DEFAULT))
    }

    val selectedItemId by viewModel.selectedItemId
    val expandedFolders by viewModel.expandedFolderIds
    val searchQuery by viewModel.searchQuery
    val visibleItems = viewModel.getVisibleItems()

    var selectedSnippetContent by remember { mutableStateOf("") }
    var copiedMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedItemId) {
        val snippet = viewModel.getSelectedSnippet()
        selectedSnippetContent = snippet?.let {
            when (it.name) {
                "backup-database.sh" -> """#!/bin/bash
# Backup script
mysqldump -u root -p database > backup.sql"""
                "nginx.conf" -> """server {
    listen 80;
    server_name example.com;
}"""
                else -> "# Snippet content for ${it.name}"
            }
        } ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DarkNote") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TreeView(
                state = TreeViewState(
                    items = visibleItems,
                    selectedItemId = selectedItemId,
                    searchQuery = searchQuery
                ),
                onItemClick = { item -> viewModel.selectItem(item.id) },
                onItemToggle = { folder -> viewModel.toggleFolder(folder.id) },
                onCreateSnippet = { viewModel.createSnippet() },
                onCreateFolder = { viewModel.createFolder() },
                modifier = Modifier.width(280.dp)
            )

            Divider(
                modifier = Modifier.fillMaxHeight().width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                if (selectedItemId != null && selectedSnippetContent.isNotEmpty()) {
                    OutlinedTextField(
                        value = selectedSnippetContent,
                        onValueChange = { selectedSnippetContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.copy(selectedSnippetContent, sanitize = true)
                                copiedMessage = "Copied (sanitized)"
                            }
                        ) {
                            Text("Copy Sanitized")
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.copy(selectedSnippetContent, sanitize = false)
                                copiedMessage = "Copied (raw)"
                            }
                        ) {
                            Text("Copy Raw")
                        }

                        copiedMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Select a snippet from the sidebar",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Or create a new one using + button",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMainScreen() {
    MaterialTheme {
        MainScreen()
    }
}
