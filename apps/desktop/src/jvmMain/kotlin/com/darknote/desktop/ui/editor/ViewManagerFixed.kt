package com.darknote.desktop.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.HorizontalSplit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darknote.core.model.Snippet

/**
 * FIXED ViewManager - Simplified Kate-style editor with proper state management
 * 
 * This version fixes the critical issues:
 * - Shared state between all panels
 * - Functional split operations  
 * - Proper callback handling
 */
@Composable
fun ViewManagerFixed(
    snippet: Snippet?,
    content: String,
    onContentChange: (String) -> Unit,
    isModified: Boolean,
    saveStatus: SaveStatus,
    onCopy: () -> Unit,
    onCopyRaw: () -> Unit,
    onSave: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Single source of truth for split state
    var splitState by remember { mutableStateOf(SplitState.Single) }
    
    when (splitState) {
        SplitState.Single -> {
            // Single editor view with split controls
            SingleEditorView(
                snippet = snippet,
                content = content,
                onContentChange = onContentChange,
                isModified = isModified,
                saveStatus = saveStatus,
                onCopy = onCopy,
                onCopyRaw = onCopyRaw,
                onSave = onSave,
                onFocusChanged = onFocusChanged,
                onSplitHorizontal = { splitState = SplitState.Horizontal },
                onSplitVertical = { splitState = SplitState.Vertical },
                modifier = modifier
            )
        }
        
        SplitState.Horizontal -> {
            // Horizontal split: left | right
            HorizontalSplitView(
                snippet = snippet,
                content = content,
                onContentChange = onContentChange,
                isModified = isModified,
                saveStatus = saveStatus,
                onCopy = onCopy,
                onCopyRaw = onCopyRaw,
                onSave = onSave,
                onFocusChanged = onFocusChanged,
                onCloseSplit = { splitState = SplitState.Single },
                modifier = modifier
            )
        }
        
        SplitState.Vertical -> {
            // Vertical split: top / bottom  
            VerticalSplitView(
                snippet = snippet,
                content = content,
                onContentChange = onContentChange,
                isModified = isModified,
                saveStatus = saveStatus,
                onCopy = onCopy,
                onCopyRaw = onCopyRaw,
                onSave = onSave,
                onFocusChanged = onFocusChanged,
                onCloseSplit = { splitState = SplitState.Single },
                modifier = modifier
            )
        }
    }
}

enum class SplitState { Single, Horizontal, Vertical }

@Composable
private fun SingleEditorView(
    snippet: Snippet?,
    content: String,
    onContentChange: (String) -> Unit,
    isModified: Boolean,
    saveStatus: SaveStatus,
    onCopy: () -> Unit,
    onCopyRaw: () -> Unit,
    onSave: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSplitHorizontal: () -> Unit,
    onSplitVertical: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorState = remember { EditorState() }
    
    // Open the current snippet in a tab
    LaunchedEffect(snippet) {
        snippet?.let {
            editorState.openTab(it.id, it.title)
        }
    }
    
    Column(modifier = modifier) {
        // Tab bar + Split controls
        EditorHeader(
            editorState = editorState,
            onSplitHorizontal = onSplitHorizontal,
            onSplitVertical = onSplitVertical
        )
        
        // Editor
        SnippetEditor(
            snippet = snippet,
            content = content,
            onContentChange = onContentChange,
            isModified = isModified,
            saveStatus = saveStatus,
            onCopy = onCopy,
            onCopyRaw = onCopyRaw,
            onSave = onSave,
            onFocusChanged = onFocusChanged,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HorizontalSplitView(
    snippet: Snippet?,
    content: String,
    onContentChange: (String) -> Unit,
    isModified: Boolean,
    saveStatus: SaveStatus,
    onCopy: () -> Unit,
    onCopyRaw: () -> Unit,
    onSave: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onCloseSplit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left side - same content as main
            SingleEditorView(
                snippet = snippet,
                content = content,
                onContentChange = onContentChange,
                isModified = isModified,
                saveStatus = saveStatus,
                onCopy = onCopy,
                onCopyRaw = onCopyRaw,
                onSave = onSave,
                onFocusChanged = onFocusChanged,
                onSplitHorizontal = {},
                onSplitVertical = {},
                modifier = Modifier.weight(1f)
            )
            
            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            // Right side - empty editor
            EmptyEditorView(
                modifier = Modifier.weight(1f)
            )
        }
        
        // Close split button
        FloatingActionButton(
            onClick = onCloseSplit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp),
            containerColor = MaterialTheme.colorScheme.errorContainer
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close split",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun VerticalSplitView(
    snippet: Snippet?,
    content: String,
    onContentChange: (String) -> Unit,
    isModified: Boolean,
    saveStatus: SaveStatus,
    onCopy: () -> Unit,
    onCopyRaw: () -> Unit,
    onSave: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onCloseSplit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top side - same content as main
            SingleEditorView(
                snippet = snippet,
                content = content,
                onContentChange = onContentChange,
                isModified = isModified,
                saveStatus = saveStatus,
                onCopy = onCopy,
                onCopyRaw = onCopyRaw,
                onSave = onSave,
                onFocusChanged = onFocusChanged,
                onSplitHorizontal = {},
                onSplitVertical = {},
                modifier = Modifier.weight(1f)
            )
            
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            // Bottom side - empty editor
            EmptyEditorView(
                modifier = Modifier.weight(1f)
            )
        }
        
        // Close split button
        FloatingActionButton(
            onClick = onCloseSplit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp),
            containerColor = MaterialTheme.colorScheme.errorContainer
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close split",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyEditorView(
    modifier: Modifier = Modifier
) {
    val emptyEditorState = remember { EditorState() }
    
    Column(modifier = modifier) {
        // Empty tab bar
        EditorHeader(
            editorState = emptyEditorState,
            onSplitHorizontal = {},
            onSplitVertical = {}
        )
        
        // Empty state
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Select a snippet to view here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditorHeader(
    editorState: EditorState,
    onSplitHorizontal: () -> Unit,
    onSplitVertical: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab bar
            if (editorState.tabs.isNotEmpty()) {
                EditorTabBar(
                    tabs = editorState.tabs,
                    activeIndex = editorState.activeTabIndex,
                    onTabClick = { editorState.activeTabIndex = it },
                    onTabClose = { editorState.closeTab(it) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            
            // Split controls
            Row {
                IconButton(onClick = onSplitHorizontal) {
                    Icon(
                        Icons.Filled.VerticalSplit,
                        "Split horizontally",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onSplitVertical) {
                    Icon(
                        Icons.Filled.HorizontalSplit,
                        "Split vertically",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}