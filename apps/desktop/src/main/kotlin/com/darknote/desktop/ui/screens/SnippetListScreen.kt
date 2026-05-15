package com.darknote.desktop.ui.screens

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.darknote.core.model.Snippet
import com.darknote.desktop.viewmodel.SnippetListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetListScreen(
    viewModel: SnippetListViewModel,
    onSnippetClick: (Snippet) -> Unit,
    onCreateSnippet: (String) -> Unit
) {
    val snippets by viewModel.filteredSnippets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val snackbarData by viewModel.snackbarData.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(snackbarData) {
        snackbarData?.let {
            snackbarHostState.showSnackbar(it.message)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DarkNote") },
                actions = {
                    // Sync status
                    when (syncState) {
                        is com.darknote.sync.engine.SyncState.Syncing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        is com.darknote.sync.engine.SyncState.Error -> {
                            Icon(Icons.Default.SyncProblem, "Sync Error", tint = MaterialTheme.colorScheme.error)
                        }
                        is com.darknote.sync.engine.SyncState.Synced -> {
                            Icon(Icons.Default.CloudDone, "Synced", tint = MaterialTheme.colorScheme.primary)
                        }
                        else -> {}
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    IconButton(onClick = { viewModel.triggerSync() }) {
                        Icon(Icons.Default.Sync, "Sync")
                    }
                    
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.createSnippet { snippetId ->
                        onCreateSnippet(snippetId)
                    }
                }
            ) {
                Icon(Icons.Default.Add, "New Snippet")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search snippets...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Snippet list
            if (snippets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Note,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isEmpty()) "No snippets yet" else "No results found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(snippets, key = { it.id }) { snippet ->
                        SnippetCard(
                            snippet = snippet,
                            onClick = { onSnippetClick(snippet) },
                            onFavoriteClick = { viewModel.toggleFavorite(snippet) },
                            onCopyClick = { viewModel.copyToClipboard(snippet) },
                            onDeleteClick = { viewModel.deleteSnippet(snippet) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetCard(
    snippet: Snippet,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = snippet.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            if (snippet.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            "Favorite",
                            tint = if (snippet.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy") },
                                onClick = {
                                    onCopyClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    onDeleteClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = snippet.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                snippet.language?.let { lang ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } ?: Spacer(Modifier.width(1.dp))
                
                Text(
                    text = dateFormat.format(Date(snippet.modifiedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
