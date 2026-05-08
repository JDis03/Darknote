@file:OptIn(ExperimentalMaterial3Api::class)

package com.darknote.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darknote.core.model.Folder
import com.darknote.core.model.Snippet

@Composable
fun RenameSnippetDialog(
    snippet: Snippet,
    onRename: (id: String, newTitle: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTitle by remember { mutableStateOf(snippet.title) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Snippet") },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Snippet title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (newTitle.isNotBlank() && newTitle != snippet.title) onRename(snippet.id, newTitle) },
                enabled = newTitle.isNotBlank() && newTitle != snippet.title
            ) { 
                Text("Rename") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel") 
            }
        }
    )
}

@Composable
fun CreateFolderDialog(
    parentFolders: List<Folder>,
    onCreate: (name: String, parentId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = parentFolders.find { it.id == selectedParentId }?.name ?: "Root (no parent)",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Parent folder") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Root (no parent)") },
                            onClick = { 
                                selectedParentId = null
                                expanded = false 
                            }
                        )
                        parentFolders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = { 
                                    selectedParentId = folder.id
                                    expanded = false 
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name, selectedParentId) },
                enabled = name.isNotBlank()
            ) { 
                Text("Create") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel") 
            }
        }
    )
}

@Composable
fun RenameFolderDialog(
    folder: Folder,
    onRename: (id: String, newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(folder.name) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Folder") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (newName.isNotBlank() && newName != folder.name) onRename(folder.id, newName) },
                enabled = newName.isNotBlank() && newName != folder.name
            ) { 
                Text("Rename") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel") 
            }
        }
    )
}

@Composable
fun DeleteFolderDialog(
    folder: Folder,
    hasChildFolders: Boolean,
    hasChildSnippets: Boolean,
    onDelete: (id: String, moveChildrenToParent: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var moveChildren by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Folder") },
        text = {
            Column {
                Text("Delete \"${folder.name}\"?")
                
                if (hasChildFolders || hasChildSnippets) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = moveChildren, 
                            onCheckedChange = { moveChildren = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Move contents to parent folder")
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onDelete(folder.id, moveChildren) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) { 
                Text("Delete") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel") 
            }
        }
    )
}