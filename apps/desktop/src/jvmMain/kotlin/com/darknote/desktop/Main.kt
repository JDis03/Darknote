package com.darknote.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.darknote.desktop.viewmodel.SnippetTreeViewModel

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DarkNote - Snippet Manager"
    ) {
        MaterialTheme {
            MainScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel = remember { SnippetTreeViewModel.create() }
    val clipboardManager = remember {
        DesktopClipboardManager(ClipboardSanitizer(ClipboardSettings.DEFAULT))
    }
    
    val selectedItemId by viewModel.selectedItemId
    val expandedFolders by viewModel.expandedFolderIds
    val searchQuery by viewModel.searchQuery
    val visibleItems = viewModel.visibleItems
    
    var selectedSnippetContent by remember { mutableStateOf("") }
    var copiedMessage by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    
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
                actions = {
                    // Sync status indicator (placeholder)
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Sync Status - Offline Mode"
                        )
                    }
                    
                    // Settings button
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
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
            // Sidebar - Tree View
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
            
            // Editor Area
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
                    // Empty state
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
        
        // Settings Dialog (placeholder for sync settings)
        if (showSettings) {
            SettingsDialog(
                onDismiss = { showSettings = false },
                onConnectDropbox = { 
                    // TODO: Open browser for OAuth
                    println("Open Dropbox OAuth in browser")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onConnectDropbox: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sync Section
                Text(
                    "Synchronization",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dropbox")
                        Text(
                            "Not connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(onClick = onConnectDropbox) {
                        Text("Connect")
                    }
                }
                
                Divider()
                
                // Info
                Text(
                    "Working in offline mode. Connect to Dropbox to sync across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Preview
@Composable
fun PreviewMainScreen() {
    MaterialTheme {
        MainScreen()
    }
}
