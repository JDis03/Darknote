package com.darknote.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darknote.desktop.viewmodel.SnippetListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class EditorSaveStatus { Idle, Saving, Saved, Error }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
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
    var isModified by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf(EditorSaveStatus.Idle) }
    val scrollState = rememberScrollState()

    LaunchedEffect(snippetId) {
        snippets.find { it.id == snippetId }?.let {
            val loaded = viewModel.loadSnippetWithContent(it)
            contentField = loaded.content
            originalContent = loaded.content
            titleField = loaded.title
            originalTitle = loaded.title
        }
    }

    LaunchedEffect(contentField) {
        if (contentField != originalContent) {
            isModified = true
            delay(1500L)
            if (contentField != originalContent && snippet != null) {
                saveStatus = EditorSaveStatus.Saving
                viewModel.updateSnippet(snippet.copy(title = titleField, content = contentField))
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
                        Text(titleField.ifBlank { "Untitled" })
                        
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
            // Info bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    text = "$lines lines · $chars chars",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = dateFormat.format(Date(snippet.modifiedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider()
            
            // Editor
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Title field
                BasicTextField(
                    value = titleField,
                    onValueChange = { titleField = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        if (titleField.isEmpty()) {
                            Text(
                                text = "Untitled",
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(12.dp))

                // Content editor
                BasicTextField(
                    value = contentField,
                    onValueChange = { contentField = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
