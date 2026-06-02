package com.darknote.android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SnippetContextMenu(
    isFavorite: Boolean,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCopyRaw: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("Edit") },
        onClick = onEdit,
        leadingIcon = {
            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
    DropdownMenuItem(
        text = { Text("Rename") },
        onClick = onRename,
        leadingIcon = {
            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
    DropdownMenuItem(
        text = { Text("Copy (sanitized)") },
        onClick = onCopy,
        leadingIcon = {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
    DropdownMenuItem(
        text = { Text("Copy raw") },
        onClick = onCopyRaw,
        leadingIcon = {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
    DropdownMenuItem(
        text = { Text(if (isFavorite) "Remove from favorites" else "Add to favorites") },
        onClick = onToggleFavorite,
        leadingIcon = {
            Icon(
                if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
    DropdownMenuItem(
        text = { Text("Share") },
        onClick = onShare,
        leadingIcon = {
            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
    DropdownMenuItem(
        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
        onClick = onDelete,
        leadingIcon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        }
    )
}

@Composable
fun FolderContextMenu(
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("Rename") },
        onClick = onRename,
        leadingIcon = {
            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
    DropdownMenuItem(
        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
        onClick = onDelete,
        leadingIcon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        }
    )
}