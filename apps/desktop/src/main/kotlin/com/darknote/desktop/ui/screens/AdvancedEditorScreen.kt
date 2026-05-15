package com.darknote.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darknote.desktop.ui.components.CodeEditor
import com.darknote.desktop.ui.components.EditorToolbar
import com.darknote.desktop.ui.components.FindReplaceDialog
import com.darknote.desktop.viewmodel.SnippetListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedEditorScreen(
    snippetId: String,
    viewModel: SnippetListViewModel,
    onBack: () -> Unit
) {
    val snippets by viewModel.filteredSnippets.collectAsState()
    val snippet = remember(snippetId, snippets) { snippets.find { it.id == snippetId } }
    val scope = rememberCoroutineScope()

    var contentField by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var titleField by remember { mutableStateOf("") }
    var originalTitle by remember { mutableStateOf("") }
    var languageField by remember { mutableStateOf<String?>(null) }
    var isModified by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf(EditorSaveStatus.Idle) }
    
    // Editor preferences
    var showLineNumbers by remember { mutableStateOf(true) }
    var wordWrap by remember { mutableStateOf(false) }
    var showFindDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snippetId) {
        snippets.find { it.id == snippetId }?.let {
            val loaded = viewModel.loadSnippetWithContent(it)
            contentField = loaded.content
            originalContent = loaded.content
            titleField = loaded.title
            originalTitle = loaded.title
            languageField = loaded.language
        }
    }

    LaunchedEffect(contentField, languageField) {
        if (contentField != originalContent || languageField != snippet?.language) {
            isModified = true
            delay(1500L)
            if ((contentField != originalContent || languageField != snippet?.language) && snippet != null) {
                saveStatus = EditorSaveStatus.Saving
                viewModel.updateSnippet(
                    snippet.copy(
                        title = titleField,
                        content = contentField,
                        language = languageField
                    )
                )
                saveStatus = EditorSaveStatus.Saved
                originalContent = contentField
                originalTitle = titleField
                isModified = false
                delay(1200L)
                if (saveStatus == EditorSaveStatus.Saved) saveStatus = EditorSaveStatus.Idle
            }
        }
    }

    LaunchedEffect(titleField) {
        if (titleField != originalTitle && titleField.isNotBlank()) {
            delay(1500L)
            if (titleField != originalTitle && snippet != null) {
                saveStatus = EditorSaveStatus.Saving
                viewModel.updateSnippet(snippet.copy(title = titleField, content = contentField))
                saveStatus = EditorSaveStatus.Saved
                originalTitle = titleField
                originalContent = contentField
                delay(1200L)
                if (saveStatus == EditorSaveStatus.Saved) saveStatus = EditorSaveStatus.Idle
            }
        }
    }

    if (snippet == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val lines = contentField.count { it == '\n' } + 1
    val chars = contentField.length

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                titleField.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "$lines lines · $chars chars",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        when (saveStatus) {
                            EditorSaveStatus.Saving -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Saving...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            EditorSaveStatus.Saved -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Saved",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            EditorSaveStatus.Error -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Error,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Error",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Text(
                        dateFormat.format(Date(snippet.modifiedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    IconButton(onClick = { viewModel.toggleFavorite(snippet) }) {
                        Icon(
                            if (snippet.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            "Favorite",
                            tint = if (snippet.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(onClick = { viewModel.copyToClipboard(snippet) }) {
                        Icon(Icons.Default.ContentCopy, "Copy")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Title field
            OutlinedTextField(
                value = titleField,
                onValueChange = { titleField = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Untitled") },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
            
            // Editor toolbar
            EditorToolbar(
                language = languageField,
                onLanguageChange = { languageField = it },
                wordWrap = wordWrap,
                onWordWrapToggle = { wordWrap = !wordWrap },
                showLineNumbers = showLineNumbers,
                onLineNumbersToggle = { showLineNumbers = !showLineNumbers },
                onFind = { showFindDialog = true },
                onFormat = {
                    // TODO: Implement code formatting
                }
            )
            
            Divider()
            
            // Code editor
            CodeEditor(
                value = contentField,
                onValueChange = { contentField = it },
                language = languageField,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                showLineNumbers = showLineNumbers
            )
        }
    }
    
    // Find/Replace dialog
    if (showFindDialog) {
        FindReplaceDialog(
            onDismiss = { showFindDialog = false },
            text = contentField,
            onFind = { query, caseSensitive, wholeWord ->
                // Count matches
                val regex = if (caseSensitive) {
                    Regex(if (wholeWord) "\\b$query\\b" else query)
                } else {
                    Regex(if (wholeWord) "\\b$query\\b" else query, RegexOption.IGNORE_CASE)
                }
                regex.findAll(contentField).count()
            },
            onReplace = { find, replace, caseSensitive, wholeWord ->
                // Replace first occurrence
                val regex = if (caseSensitive) {
                    Regex(if (wholeWord) "\\b$find\\b" else find)
                } else {
                    Regex(if (wholeWord) "\\b$find\\b" else find, RegexOption.IGNORE_CASE)
                }
                val newContent = regex.replaceFirst(contentField, replace)
                contentField = newContent
                regex.findAll(newContent).count()
            },
            onReplaceAll = { find, replace, caseSensitive, wholeWord ->
                // Replace all occurrences
                val regex = if (caseSensitive) {
                    Regex(if (wholeWord) "\\b$find\\b" else find)
                } else {
                    Regex(if (wholeWord) "\\b$find\\b" else find, RegexOption.IGNORE_CASE)
                }
                val count = regex.findAll(contentField).count()
                contentField = regex.replace(contentField, replace)
                count
            }
        )
    }
}
