package com.darknote.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darknote.android.SnippetListViewModel
import com.darknote.core.model.Snippet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

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
    var showMoreSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

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
            CircularProgressIndicator(strokeWidth = 2.dp)
        }
        return
    }

    val folderName = snippet.folderId?.let { fid -> folders.find { it.id == fid }?.name }
    val lines = content.lines().size
    val chars = content.length

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    when (saveStatus) {
                        EditorSaveStatus.Saving -> Icon(
                            Icons.Default.Sync, "Saving...",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        EditorSaveStatus.Saved -> Icon(
                            Icons.Default.CheckCircle, "Saved",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        EditorSaveStatus.Error -> Icon(
                            Icons.Default.Error, "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        EditorSaveStatus.Idle -> if (isModified) Text(
                            "●", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.width(4.dp))

                    IconButton(onClick = { viewModel.copySnippet(snippet) }) {
                        Icon(Icons.Default.ContentCopy, "Copy")
                    }
                    IconButton(onClick = { showMoreSheet = true }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 1.dp, color = MaterialTheme.colorScheme.surface) {
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
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.surface
        ) {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Tags row
                    if (snippet.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            snippet.tags.forEach { tag ->
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Editable title
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Content editor
                    BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }

    // Bottom Sheet with actions
    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Copy raw
                ListItem(
                    headlineContent = { Text("Copy raw content") },
                    leadingContent = { Icon(Icons.Default.ContentPaste, null) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.copyRawSnippet(snippet)
                    }
                )
                // Favorite toggle
                ListItem(
                    headlineContent = { Text(if (snippet.isFavorite) "Remove from favorites" else "Add to favorites") },
                    leadingContent = {
                        Icon(
                            if (snippet.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                            null
                        )
                    },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.toggleFavorite(snippet)
                    }
                )
                // Share
                ListItem(
                    headlineContent = { Text("Share snippet") },
                    leadingContent = { Icon(Icons.Default.Share, null) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.shareSnippet(snippet, android.app.Activity())
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                // Delete
                ListItem(
                    headlineContent = {
                        Text("Delete snippet", color = MaterialTheme.colorScheme.error)
                    },
                    leadingContent = {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.deleteSnippet(snippet)
                        onBack()
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(300L)
        focusRequester.requestFocus()
    }
}
