package com.darknote.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darknote.core.model.Folder

/**
 * Sidebar showing folder tree for snippet organization.
 * Similar to Kate/KWrite sidebar pattern.
 */
@Composable
fun FolderSidebar(
    folders: List<Folder>,
    selectedFolderId: String?,
    onFolderSelect: (String?) -> Unit,
    onCreateFolder: () -> Unit,
    onRenameFolder: (Folder) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var contextMenuFolder by remember { mutableStateOf<Folder?>(null) }

    Surface(
        modifier = modifier.width(240.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Folders",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onCreateFolder,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, "New Folder", modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider()

            // "All Snippets" item
            FolderItem(
                name = "All Snippets",
                icon = Icons.Default.Folder,
                isSelected = selectedFolderId == null,
                onClick = { onFolderSelect(null) },
                depth = 0
            )

            HorizontalDivider()

            // Folder tree
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                val rootFolders = folders.filter { it.parentId == null }.sortedBy { it.name }
                items(rootFolders, key = { it.id }) { folder ->
                    FolderTreeItem(
                        folder = folder,
                        allFolders = folders,
                        selectedFolderId = selectedFolderId,
                        expandedFolders = expandedFolders,
                        onFolderSelect = onFolderSelect,
                        onToggleExpand = { folderId ->
                            expandedFolders = if (folderId in expandedFolders) {
                                expandedFolders - folderId
                            } else {
                                expandedFolders + folderId
                            }
                        },
                        onContextMenu = { contextMenuFolder = it },
                        depth = 0
                    )
                }
            }
        }
    }

    // Context menu for folder actions
    contextMenuFolder?.let { folder ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { contextMenuFolder = null }
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    onRenameFolder(folder)
                    contextMenuFolder = null
                },
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDeleteFolder(folder)
                    contextMenuFolder = null
                },
                leadingIcon = { Icon(Icons.Default.Delete, null) }
            )
        }
    }
}

@Composable
private fun FolderTreeItem(
    folder: Folder,
    allFolders: List<Folder>,
    selectedFolderId: String?,
    expandedFolders: Set<String>,
    onFolderSelect: (String?) -> Unit,
    onToggleExpand: (String) -> Unit,
    onContextMenu: (Folder) -> Unit,
    depth: Int
) {
    val hasChildren = allFolders.any { it.parentId == folder.id }
    val isExpanded = folder.id in expandedFolders

    Column {
        FolderItem(
            name = folder.name,
            icon = if (hasChildren) {
                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
            } else {
                Icons.Default.Folder
            },
            isSelected = selectedFolderId == folder.id,
            onClick = { onFolderSelect(folder.id) },
            onExpandClick = if (hasChildren) {
                { onToggleExpand(folder.id) }
            } else null,
            onContextMenu = { onContextMenu(folder) },
            depth = depth
        )

        // Render children if expanded
        if (isExpanded && hasChildren) {
            val children = allFolders.filter { it.parentId == folder.id }.sortedBy { it.name }
            children.forEach { child ->
                FolderTreeItem(
                    folder = child,
                    allFolders = allFolders,
                    selectedFolderId = selectedFolderId,
                    expandedFolders = expandedFolders,
                    onFolderSelect = onFolderSelect,
                    onToggleExpand = onToggleExpand,
                    onContextMenu = onContextMenu,
                    depth = depth + 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderItem(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    onExpandClick: (() -> Unit)? = null,
    onContextMenu: (() -> Unit)? = null,
    depth: Int
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(start = (16 + depth * 16).dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onExpandClick != null) {
            IconButton(
                onClick = onExpandClick,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(24.dp))
        }

        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(8.dp))

        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (onContextMenu != null) {
            IconButton(
                onClick = onContextMenu,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
