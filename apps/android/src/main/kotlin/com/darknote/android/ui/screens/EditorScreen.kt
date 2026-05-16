package com.darknote.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darknote.android.SnippetListViewModel
import kotlinx.coroutines.delay
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
    val folders  by viewModel.folders.collectAsState()
    val snippet  = remember(snippetId, snippets) { snippets.find { it.id == snippetId } }

    // TextFieldValue state — rememberSaveable preserves cursor across rotation.
    // snippetId as key resets state when navigating to a different snippet.
    var titleField   by rememberSaveable(snippetId, stateSaver = TextFieldValueSaver) { mutableStateOf(TextFieldValue("")) }
    var contentField by rememberSaveable(snippetId, stateSaver = TextFieldValueSaver) { mutableStateOf(TextFieldValue("")) }

    // "Original" values are used to detect unsaved changes.
    // Also rememberSaveable so they survive rotation without re-triggering auto-save.
    var originalTitle   by rememberSaveable(snippetId) { mutableStateOf("") }
    var originalContent by rememberSaveable(snippetId) { mutableStateOf("") }

    // Tracks whether we already loaded snippet data into the fields.
    // Prevents re-loading (and cursor reset) on every recomposition / rotation.
    var hasInitialized by rememberSaveable(snippetId) { mutableStateOf(false) }

    var isModified  by remember { mutableStateOf(false) }
    var saveStatus  by remember { mutableStateOf(EditorSaveStatus.Idle) }
    var showMoreSheet by remember { mutableStateOf(false) }

    val titleFocusRequester   = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // ── Initial load ──────────────────────────────────────────────────────────
    // Runs once per snippetId (and not again after rotation thanks to hasInitialized).
    // Sets TextFieldValue FIRST, then requests focus so the IME gets the correct
    // cursor position from the start — avoiding the "cursor at 0" race condition.
    LaunchedEffect(snippetId) {
        if (hasInitialized) {
            // Already loaded (e.g. screen rotated). Fields restored from rememberSaveable.
            // Just re-request focus so the keyboard reappears.
            titleFocusRequester.requestFocus()
            return@LaunchedEffect
        }

        val it = snippets.find { it.id == snippetId } ?: return@LaunchedEffect
        val loaded = viewModel.loadSnippetWithContent(it)

        titleField = TextFieldValue(
            text = loaded.title,
            selection = TextRange(loaded.title.length) // cursor at end
        )
        contentField = TextFieldValue(
            text = loaded.content,
            selection = TextRange(loaded.content.length)
        )
        originalTitle   = loaded.title
        originalContent = loaded.content
        hasInitialized  = true

        // Request focus AFTER setting the value so the IME receives the correct
        // cursor position immediately — not via an external update while focused.
        titleFocusRequester.requestFocus()
    }

    // ── Auto-save ─────────────────────────────────────────────────────────────
    // Debounce: save 1.5 s after the last change to either title or content.
    LaunchedEffect(titleField.text, contentField.text) {
        val titleChanged   = titleField.text != originalTitle
        val contentChanged = contentField.text != originalContent

        if (!titleChanged && !contentChanged) return@LaunchedEffect
        if (titleField.text.isBlank()) return@LaunchedEffect

        isModified = true
        delay(1500L)

        // Re-check after debounce (user might have reverted)
        val stillTitleChanged   = titleField.text != originalTitle
        val stillContentChanged = contentField.text != originalContent
        if ((!stillTitleChanged && !stillContentChanged) || snippet == null) return@LaunchedEffect

        saveStatus = EditorSaveStatus.Saving
        viewModel.updateSnippet(
            snippet.copy(title = titleField.text, content = contentField.text)
        )
        originalTitle   = titleField.text
        originalContent = contentField.text
        isModified      = false
        saveStatus      = EditorSaveStatus.Saved
        delay(1200L)
        if (saveStatus == EditorSaveStatus.Saved) saveStatus = EditorSaveStatus.Idle
    }

    // ── Loading state ─────────────────────────────────────────────────────────
    if (snippet == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        }
        return
    }

    val folderName = snippet.folderId?.let { fid -> folders.find { it.id == fid }?.name }
    val lines = contentField.text.lines().size
    val chars = contentField.text.length

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = titleField.text.ifBlank { "Untitled" },
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onSurface)
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
                            "●",
                            color = MaterialTheme.colorScheme.primary,
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
                        "$lines lines · $chars chars",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        dateFormat.format(Date(snippet.modifiedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.surface
        ) {
            // NOTE: SelectionContainer must NOT wrap BasicTextField — it conflicts
            // with BasicTextField's own selection management and resets the cursor.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Tags
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

                // Title field
                // keyboardOptions = Next so Enter moves focus to content
                BasicTextField(
                    value = titleField,
                    onValueChange = { titleField = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { contentFocusRequester.requestFocus() }
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (titleField.text.isEmpty()) {
                            Text(
                                text = "Untitled",
                                style = MaterialTheme.typography.headlineSmall.copy(
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

                // Content field
                BasicTextField(
                    value = contentField,
                    onValueChange = { contentField = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(contentFocusRequester),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )

                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // More actions sheet
    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Copy raw content") },
                    leadingContent = { Icon(Icons.Default.ContentPaste, null) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.copyRawSnippet(snippet)
                    }
                )
                ListItem(
                    headlineContent = {
                        Text(if (snippet.isFavorite) "Remove from favorites" else "Add to favorites")
                    },
                    leadingContent = {
                        Icon(if (snippet.isFavorite) Icons.Default.Star else Icons.Default.StarOutline, null)
                    },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.toggleFavorite(snippet)
                    }
                )
                val context = LocalContext.current
                ListItem(
                    headlineContent = { Text("Share snippet") },
                    leadingContent = { Icon(Icons.Default.Share, null) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.shareSnippet(snippet, context)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
}

// Saver that preserves both text content and cursor position across
// configuration changes (rotation, process death).
private val TextFieldValueSaver = Saver<TextFieldValue, String>(
    save = { "${it.selection.start},${it.selection.end}|${it.text}" },
    restore = { saved ->
        val sep  = saved.indexOf('|')
        val text = if (sep >= 0) saved.substring(sep + 1) else saved
        val selection = runCatching {
            val parts = saved.substring(0, sep).split(",")
            val start = parts[0].toInt().coerceIn(0, text.length)
            val end   = parts[1].toInt().coerceIn(0, text.length)
            TextRange(start, end)
        }.getOrElse { TextRange(text.length) }
        TextFieldValue(text = text, selection = selection)
    }
)
