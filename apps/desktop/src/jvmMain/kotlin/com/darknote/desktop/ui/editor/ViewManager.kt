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
 * ViewManager follows Kate's recursive splitter pattern.
 * A ViewManager IS-A splitter that can contain either:
 * - Two child ViewManagers (splitter node)
 * - A single ViewSpace (leaf node)
 */
@Composable
fun ViewManager(
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
    onCloseSplit: () -> Unit,
    modifier: Modifier = Modifier,
    state: ViewManagerState = remember { ViewManagerState() }
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state.type) {
            ViewType.Single -> {
                // Single ViewSpace (leaf node)
                ViewSpace(
                    snippet = snippet,
                    content = content,
                    onContentChange = onContentChange,
                    isModified = isModified,
                    saveStatus = saveStatus,
                    onCopy = onCopy,
                    onCopyRaw = onCopyRaw,
                    onSave = onSave,
                    onFocusChanged = onFocusChanged,
                    onSplitHorizontal = { state.splitHorizontal() },
                    onSplitVertical = { state.splitVertical() },
                    showSplitControls = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ViewType.HorizontalSplit -> {
                // Horizontal split: left | right
                Row(modifier = Modifier.fillMaxSize()) {
                    ViewManager(
                        snippet = snippet,
                        content = content,
                        onContentChange = onContentChange,
                        isModified = isModified,
                        saveStatus = saveStatus,
                        onCopy = onCopy,
                        onCopyRaw = onCopyRaw,
                        onSave = onSave,
                        onFocusChanged = onFocusChanged,
                        onSplitHorizontal = onSplitHorizontal,
                        onSplitVertical = onSplitVertical,
                        onCloseSplit = { state.unsplit() },
                        state = state.left,
                        modifier = Modifier.weight(1f)
                    )
                    
                    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    
                    ViewManager(
                        snippet = null, // Right side starts empty
                        content = "",
                        onContentChange = {},
                        isModified = false,
                        saveStatus = SaveStatus.Idle,
                        onCopy = {},
                        onCopyRaw = {},
                        onSave = {},
                        onFocusChanged = {},
                        onSplitHorizontal = onSplitHorizontal,
                        onSplitVertical = onSplitVertical,
                        onCloseSplit = { state.unsplit() },
                        state = state.right,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            ViewType.VerticalSplit -> {
                // Vertical split: top / bottom
                Column(modifier = Modifier.fillMaxSize()) {
                    ViewManager(
                        snippet = snippet,
                        content = content,
                        onContentChange = onContentChange,
                        isModified = isModified,
                        saveStatus = saveStatus,
                        onCopy = onCopy,
                        onCopyRaw = onCopyRaw,
                        onSave = onSave,
                        onFocusChanged = onFocusChanged,
                        onSplitHorizontal = onSplitHorizontal,
                        onSplitVertical = onSplitVertical,
                        onCloseSplit = { state.unsplit() },
                        state = state.left,
                        modifier = Modifier.weight(1f)
                    )
                    
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    
                    ViewManager(
                        snippet = null, // Bottom side starts empty
                        content = "",
                        onContentChange = {},
                        isModified = false,
                        saveStatus = SaveStatus.Idle,
                        onCopy = {},
                        onCopyRaw = {},
                        onSave = {},
                        onFocusChanged = {},
                        onSplitHorizontal = onSplitHorizontal,
                        onSplitVertical = onSplitVertical,
                        onCloseSplit = { state.unsplit() },
                        state = state.right,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Close split button (only show in split mode)
        if (state.type != ViewType.Single) {
            FloatingActionButton(
                onClick = { state.unsplit() },
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
}

/**
 * ViewSpace represents a single editor area with tabs.
 * Each ViewSpace contains:
 * - TabBar for multiple tabs  
 * - SnippetEditor for editing
 */
@Composable
private fun ViewSpace(
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
    showSplitControls: Boolean = false,
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
        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
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
                if (showSplitControls) {
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
        
        // Editor area
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

/**
 * State management for ViewManager split tree.
 * Follows Kate's pattern where ViewManager IS-A splitter.
 */
enum class ViewType { Single, HorizontalSplit, VerticalSplit }

class ViewManagerState {
    var type by mutableStateOf(ViewType.Single)
    
    // Child states for split mode
    val left = ViewManagerState()  
    val right = ViewManagerState()
    
    fun splitHorizontal() {
        if (type == ViewType.Single) {
            type = ViewType.HorizontalSplit
        }
    }
    
    fun splitVertical() {
        if (type == ViewType.Single) {
            type = ViewType.VerticalSplit
        }
    }
    
    fun unsplit() {
        type = ViewType.Single
        left.type = ViewType.Single
        right.type = ViewType.Single
    }
}