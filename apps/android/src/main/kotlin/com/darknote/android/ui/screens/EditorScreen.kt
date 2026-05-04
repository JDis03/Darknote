package com.darknote.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.darknote.android.SnippetListViewModel
import com.darknote.core.model.Snippet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class EditorSaveStatus { Idle, Saving, Saved, Error }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    snippetId: String,
    viewModel: SnippetListViewModel,
    onBack: () -> Unit
) {
    val snippets by viewModel.filteredSnippets.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val snippet = remember(snippetId, snippets) { snippets.find { it.id == snippetId } }
    val scope = rememberCoroutineScope()

    var content by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var isModified by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf(EditorSaveStatus.Idle) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(snippet) {
        snippet?.let {
            val loaded = viewModel.loadSnippetWithContent(it)
            content = loaded.content
            originalContent = loaded.content
            title = loaded.title
        }
    }

    LaunchedEffect(content) {
        if (content != originalContent) {
            isModified = true
            delay(1500L)
            if (content != originalContent && snippet != null) {
                saveStatus = EditorSaveStatus.Saving
                viewModel.updateSnippet(snippet.copy(content = content))
                saveStatus = EditorSaveStatus.Saved
                originalContent = content
                isModified = false
                delay(1200L)
                if (saveStatus == EditorSaveStatus.Saved) saveStatus = EditorSaveStatus.Idle
            }
        }
    }

    if (snippet == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val folderName = snippet.folderId?.let { fid -> folders.find { it.id == fid }?.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (folderName != null) {
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Save status indicator
                    when (saveStatus) {
                        EditorSaveStatus.Saving -> {
                            Icon(
                                Icons.Default.Sync,
                                "Saving...",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        EditorSaveStatus.Saved -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Saved",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        EditorSaveStatus.Error -> {
                            Icon(
                                Icons.Default.Error,
                                "Error saving",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        EditorSaveStatus.Idle -> {
                            if (isModified) {
                                Text(
                                    "●",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))

                    // Copy sanitized
                    IconButton(onClick = {
                        viewModel.copySnippet(snippet)
                    }) {
                        Icon(Icons.Default.ContentCopy, "Copy sanitized")
                    }

                    // Actions menu
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy raw") },
                                onClick = {
                                    showMenu = false
                                    viewModel.copyRawSnippet(snippet)
                                },
                                leadingIcon = { Icon(Icons.Default.ContentPaste, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (snippet.isFavorite) "Remove favorite" else "Add favorite") },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleFavorite(snippet)
                                },
                                leadingIcon = {
                                    Icon(
                                        if (snippet.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                                        null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showMenu = false
                                    // TODO: share from context
                                },
                                leadingIcon = { Icon(Icons.Default.Share, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    viewModel.deleteSnippet(snippet)
                                    onBack()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surface
        ) {
            SelectionContainer {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .padding(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(300L)
        focusRequester.requestFocus()
    }
}
