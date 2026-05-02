@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.darknote.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.darknote.core.model.Folder
import com.darknote.core.model.Snippet

@Composable
fun SnippetDetailSheet(
    snippet: Snippet,
    onDismiss: () -> Unit,
    onEdit: (Snippet) -> Unit,
    onCopy: (Snippet) -> Unit,
    onCopyRaw: (Snippet) -> Unit,
    onToggleFavorite: (Snippet) -> Unit,
    onDelete: (Snippet) -> Unit,
    onShare: (Snippet) -> Unit,
    folderName: String? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = snippet.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(onClick = { onToggleFavorite(snippet) }) {
                    Icon(
                        imageVector = if (snippet.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (snippet.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (snippet.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (snippet.language != null || folderName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (snippet.language != null) {
                        LanguageBadge(snippet.language)
                    }
                    if (folderName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (snippet.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(snippet.tags) { tag ->
                        androidx.compose.material3.AssistChip(
                            onClick = { },
                            label = {
                                Text("#$tag", style = MaterialTheme.typography.labelSmall)
                            }
                        )
                    }
                }
            }

            SurfaceWithContent(snippet)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onCopy(snippet) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy")
                }

                OutlinedButton(
                    onClick = { onCopyRaw(snippet) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Raw")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onEdit(snippet) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }

                OutlinedButton(
                    onClick = { onShare(snippet) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { onDelete(snippet) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete")
            }
        }
    }
}

@Composable
private fun SurfaceWithContent(snippet: Snippet) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = snippet.content,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
fun CreateSnippetSheet(
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onCreate: (title: String, content: String, language: String?, tags: List<String>, folderId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val languages = listOf("bash", "python", "kotlin", "sql", "javascript", "typescript", "config", "text")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "New Snippet",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("backup-script.sh") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            if (folders.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders) { folder ->
                        val isSelected = selectedFolderId == folder.id
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = { selectedFolderId = if (isSelected) null else folder.id },
                            label = { Text(folder.name, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(languages) { lang ->
                    val isSelected = language == lang
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = { language = if (isSelected) "" else lang },
                        label = { Text(lang, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated)") },
                placeholder = { Text("docker, backup, server") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                placeholder = { Text("#!/bin/bash\necho 'Hello World'") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                shape = MaterialTheme.shapes.medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                FilledTonalButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            val tagList = tags.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            onCreate(
                                title,
                                content,
                                language.ifBlank { null },
                                tagList,
                                selectedFolderId
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create")
                }
            }
        }
    }
}

@Composable
fun EditSnippetSheet(
    snippet: Snippet,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onSave: (Snippet) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(snippet.title) }
    var content by remember { mutableStateOf(snippet.content) }
    var language by remember { mutableStateOf(snippet.language ?: "") }
    var tags by remember { mutableStateOf(snippet.tags.joinToString(", ")) }
    var selectedFolderId by remember { mutableStateOf(snippet.folderId) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val languages = listOf("bash", "python", "kotlin", "sql", "javascript", "typescript", "config", "text")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Edit Snippet",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            if (folders.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders) { folder ->
                        val isSelected = selectedFolderId == folder.id
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = { selectedFolderId = if (isSelected) null else folder.id },
                            label = { Text(folder.name, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(languages) { lang ->
                    val isSelected = language == lang
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = { language = if (isSelected) "" else lang },
                        label = { Text(lang, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                shape = MaterialTheme.shapes.medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                FilledTonalButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            val tagList = tags.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            onSave(
                                snippet.copy(
                                    title = title,
                                    content = content,
                                    language = language.ifBlank { null },
                                    tags = tagList,
                                    folderId = selectedFolderId,
                                    modifiedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    }
}