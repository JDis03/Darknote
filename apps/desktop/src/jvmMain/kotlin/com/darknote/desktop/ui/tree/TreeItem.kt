package com.darknote.desktop.ui.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

sealed class TreeItem {
    abstract val id: String
    abstract val name: String
    abstract val isSelected: Boolean

    data class FolderItem(
        override val id: String,
        override val name: String,
        val isExpanded: Boolean = false,
        override val isSelected: Boolean = false,
        val childCount: Int = 0,
        val parentId: String? = null
    ) : TreeItem()

    data class SnippetItem(
        override val id: String,
        override val name: String,
        val language: String? = null,
        val isFavorite: Boolean = false,
        override val isSelected: Boolean = false
    ) : TreeItem()
}

@Composable
fun TreeItemView(
    item: TreeItem,
    onClick: () -> Unit,
    onToggleExpand: () -> Unit = {},
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (item.isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    val contentColor = if (item.isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    when (item) {
        is TreeItem.FolderItem -> {
            FolderItemView(
                folder = item,
                backgroundColor = backgroundColor,
                contentColor = contentColor,
                onClick = onClick,
                onToggleExpand = onToggleExpand,
                onRename = onRename,
                onDelete = onDelete,
                modifier = modifier
            )
        }
        is TreeItem.SnippetItem -> {
            SnippetItemView(
                snippet = item,
                backgroundColor = backgroundColor,
                contentColor = contentColor,
                onClick = onClick,
                onRename = onRename,
                onDelete = onDelete,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun FolderItemView(
    folder: TreeItem.FolderItem,
    backgroundColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onToggleExpand: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    
    Box {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Expand/collapse icon with its own click handler
        Icon(
            imageVector = if (folder.isExpanded)
                Icons.Default.ExpandLess
            else
                Icons.Default.ExpandMore,
            contentDescription = if (folder.isExpanded) "Collapse" else "Expand",
            modifier = Modifier
                .size(20.dp)
                .clickable(
                    onClick = onToggleExpand,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ),
            tint = contentColor
        )

        Spacer(modifier = Modifier.width(2.dp))

        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = "Folder",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

            if (folder.childCount > 0) {
                Text(
                    text = "${folder.childCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        
        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            onRename?.let {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        showContextMenu = false
                        it()
                    }
                )
            }
            onDelete?.let {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showContextMenu = false
                        it()
                    }
                )
            }
        }
    }
}

@Composable
private fun SnippetItemView(
    snippet: TreeItem.SnippetItem,
    backgroundColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    
    Box {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Spacer(modifier = Modifier.width(24.dp))

        val icon = when (snippet.language) {
            "bash", "sh", "shell" -> Icons.Default.Terminal
            "python" -> Icons.Default.Code
            "kotlin" -> Icons.Default.Android
            else -> Icons.Default.Description
        }

        Icon(
            imageVector = icon,
            contentDescription = "Snippet",
            modifier = Modifier.size(16.dp),
            tint = if (snippet.isSelected)
                contentColor
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = snippet.name,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

            if (snippet.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            onRename?.let {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        showContextMenu = false
                        it()
                    }
                )
            }
            onDelete?.let {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showContextMenu = false
                        it()
                    }
                )
            }
        }
    }
}
