package com.darknote.desktop.ui.tree

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
    modifier: Modifier = Modifier
) {
    val contentColor = if (item.isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (item) {
            is TreeItem.FolderItem -> {
                Icon(
                    imageVector = if (item.isExpanded)
                        Icons.Default.ExpandLess
                    else
                        Icons.Default.ExpandMore,
                    contentDescription = if (item.isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onToggleExpand),
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
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (item.childCount > 0) {
                    Text(
                        text = "${item.childCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            is TreeItem.SnippetItem -> {
                Spacer(modifier = Modifier.width(24.dp))

                val icon = when (item.language) {
                    "bash", "sh", "shell" -> Icons.Default.Terminal
                    "python" -> Icons.Default.Code
                    "kotlin" -> Icons.Default.Android
                    else -> Icons.Default.Description
                }

                Icon(
                    imageVector = icon,
                    contentDescription = "Snippet",
                    modifier = Modifier.size(16.dp),
                    tint = if (item.isSelected)
                        contentColor
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (item.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
