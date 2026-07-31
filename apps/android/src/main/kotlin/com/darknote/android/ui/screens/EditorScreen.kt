package com.darknote.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darknote.android.SnippetListViewModel
import com.darknote.android.ui.components.SyntaxHighlightTransformation
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
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
    var showPreview  by remember { mutableStateOf(false) }

    // WYSIWYG editor state (eye mode). contentField.text remains the single
    // source of truth (raw markdown) — richTextState is just a live view over
    // it, loaded via setMarkdown() when entering eye mode and pushed back via
    // toMarkdown() on every edit while eye mode is active. See the two
    // LaunchedEffects below.
    val richTextState = rememberRichTextState()

    // Guards the write-back sync effect against echoing the reformatting that
    // setMarkdown()/toMarkdown() round-tripping introduces (blockquote spacing,
    // italic marker normalization, etc.) right after WE load content into the
    // rich editor — as opposed to the user actually typing/formatting. Without
    // this, pasting markdown then switching into WYSIWYG mode would silently
    // rewrite the pasted text into the library's reformatted version the
    // instant it loaded, even though the user never touched anything.
    var suppressNextRichTextSync by remember(snippetId) { mutableStateOf(false) }

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
    val isMarkdown = snippet.language?.lowercase() in setOf("markdown", "md")

    // ── WYSIWYG sync ──────────────────────────────────────────────────────────
    // The rich WYSIWYG editor (RichTextEditor) is the DEFAULT view for markdown
    // notes (showPreview == false) — it's "the editor" the eye icon represents
    // being in. Toggling to showPreview == true switches to the raw/pencil
    // BasicTextField source view instead. useRichEditor below drives both the
    // content block and these two sync effects.
    val useRichEditor = isMarkdown && !showPreview

    // Entering WYSIWYG: load the current raw markdown into the rich editor.
    // Also re-runs once `hasInitialized` flips true, so the async snippet load
    // (LaunchedEffect(snippetId) above) reaches the rich editor even though it
    // completes AFTER this composable's very first frame, where contentField.text
    // is still "".
    LaunchedEffect(useRichEditor, hasInitialized) {
        if (useRichEditor && hasInitialized) {
            // Mark the NEXT annotatedString change (the one setMarkdown itself
            // is about to cause) as a load-echo, not a real edit, so the
            // write-back effect below skips it instead of bouncing the
            // reformatted markdown straight back into contentField.
            suppressNextRichTextSync = true
            richTextState.setMarkdown(contentField.text)
        }
    }

    // While the WYSIWYG editor is active, every rich-text edit is converted
    // back to markdown and pushed into contentField — the same field the
    // pencil-mode BasicTextField and the auto-save effect above read from.
    // This keeps contentField.text as the single source of truth at all times.
    //
    // toMarkdown() can reformat whitespace slightly differently from the
    // input it was given (list marker spacing, blockquote/italic normalization,
    // etc.), so besides the trimEnd() comparison below, we skip entirely the
    // one run right after setMarkdown() loaded content — see
    // suppressNextRichTextSync above. Without that guard, this effect would
    // "correct" the user's just-pasted/just-loaded text into the library's
    // own re-rendering of it the instant WYSIWYG mode opened, before the user
    // ever touched anything.
    LaunchedEffect(richTextState.annotatedString) {
        if (!useRichEditor) return@LaunchedEffect
        if (suppressNextRichTextSync) {
            suppressNextRichTextSync = false
            return@LaunchedEffect
        }
        val markdown = richTextState.toMarkdown()
        if (markdown.trimEnd() != contentField.text.trimEnd()) {
            contentField = TextFieldValue(text = markdown, selection = TextRange(markdown.length))
        }
    }

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
                    // Eye/pencil mode toggle — only meaningful for markdown notes.
                    // Eye mode is an editable WYSIWYG editor (RichTextEditor, backed
                    // by richeditor-compose's markdown parser); pencil mode is the
                    // raw BasicTextField with syntax highlighting. contentField.text
                    // is kept as the single source of truth in both modes — see the
                    // WYSIWYG sync LaunchedEffects above.
                    if (isMarkdown) {
                        IconButton(onClick = { showPreview = !showPreview }) {
                            Icon(
                                if (showPreview) Icons.Default.Edit else Icons.Default.Visibility,
                                contentDescription = if (showPreview) "Edit" else "Preview",
                                tint = if (showPreview) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                        onNext = {
                            // contentFocusRequester is attached to whichever content
                            // composable is currently mounted — RichTextEditor in WYSIWYG
                            // mode, or the raw BasicTextField otherwise (see .focusRequester
                            // on both below) — so exactly one of them always has it. Only
                            // runCatching is needed as a safety net for composition-timing
                            // edge cases (e.g. requestFocus() called before that frame's
                            // node attaches).
                            runCatching { contentFocusRequester.requestFocus() }
                        }
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

                // Content: editable WYSIWYG markdown editor (default for markdown
                // notes) or raw editor with live syntax highlighting (toggled via
                // the eye/pencil icon, or always for non-markdown notes).
                if (useRichEditor) {
                    // DEBUG: show border to confirm RichTextEditor is composed.
                    // Remove after confirming rendering works.
                    val debugContent = remember { derivedStateOf { richTextState.annotatedString.text } }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Color(0xFFE53935))
                            .padding(4.dp)
                    ) {
                        Text(
                            "RICH EDITOR ACTIVE | text len=${debugContent.value.length} | contentField len=${contentField.text.length}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE53935)),
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                    FormatToolbar(richTextState)
                    Spacer(Modifier.height(4.dp))
                    RichTextEditor(
                        state = richTextState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(contentFocusRequester),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                } else {
                    // visualTransformation applies live syntax highlighting driven by
                    // snippet.language. It's a pure display-layer transform (identity
                    // offset mapping) — the raw TextFieldValue.text is never touched,
                    // so cursor position, selection, and paste all behave exactly as
                    // if there were no highlighting at all. See SyntaxHighlightTransformation.
                    val highlightTransformation = remember(snippet.language) {
                        SyntaxHighlightTransformation(snippet.language)
                    }
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
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = highlightTransformation
                    )
                }

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

/**
 * Formatting toolbar shown above the WYSIWYG editor (eye mode only).
 * Only uses RichTextState methods documented as stable across versions:
 * toggleSpanStyle, toggleCodeSpan, toggleOrderedList, toggleUnorderedList.
 * Deliberately omits heading buttons — heading support is not confirmed
 * stable on the Kotlin-2.0-compatible library version this project pins.
 */
@Composable
private fun FormatToolbar(state: com.mohamedrejeb.richeditor.model.RichTextState) {
    val currentSpanStyle = state.currentSpanStyle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ToolbarToggle(
            icon = Icons.Default.FormatBold,
            label = "Bold",
            active = currentSpanStyle.fontWeight == FontWeight.Bold,
            onClick = { state.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) }
        )
        ToolbarToggle(
            icon = Icons.Default.FormatItalic,
            label = "Italic",
            active = currentSpanStyle.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic,
            onClick = { state.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) }
        )
        ToolbarToggle(
            icon = Icons.Default.FormatStrikethrough,
            label = "Strikethrough",
            active = currentSpanStyle.textDecoration == androidx.compose.ui.text.style.TextDecoration.LineThrough,
            onClick = { state.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) }
        )
        ToolbarToggle(
            icon = Icons.Default.Code,
            label = "Code",
            active = state.isCodeSpan,
            onClick = { state.toggleCodeSpan() }
        )
        ToolbarToggle(
            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
            label = "Bulleted list",
            active = state.isUnorderedList,
            onClick = { state.toggleUnorderedList() }
        )
        ToolbarToggle(
            icon = Icons.Default.FormatListNumbered,
            label = "Numbered list",
            active = state.isOrderedList,
            onClick = { state.toggleOrderedList() }
        )
    }
}

@Composable
private fun ToolbarToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
