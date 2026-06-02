package com.darknote.desktop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Find and Replace dialog for code editor.
 * Similar to Kate's find/replace feature.
 */
@Composable
fun FindReplaceDialog(
    onDismiss: () -> Unit,
    text: String,
    onFind: (String, Boolean, Boolean) -> Int,
    onReplace: (String, String, Boolean, Boolean) -> Int,
    onReplaceAll: (String, String, Boolean, Boolean) -> Int
) {
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var wholeWord by remember { mutableStateOf(false) }
    var showReplace by remember { mutableStateOf(false) }
    var matchCount by remember { mutableStateOf<Int?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(500.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showReplace) "Find and Replace" else "Find",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                HorizontalDivider()
                
                // Find field
                OutlinedTextField(
                    value = findText,
                    onValueChange = {
                        findText = it
                        if (it.isNotEmpty()) {
                            matchCount = onFind(it, caseSensitive, wholeWord)
                        } else {
                            matchCount = null
                        }
                    },
                    label = { Text("Find") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (matchCount != null) {
                            Text(
                                "$matchCount matches",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Replace field (if enabled)
                if (showReplace) {
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        label = { Text("Replace with") },
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = caseSensitive,
                            onCheckedChange = {
                                caseSensitive = it
                                if (findText.isNotEmpty()) {
                                    matchCount = onFind(findText, caseSensitive, wholeWord)
                                }
                            }
                        )
                        Text("Case sensitive", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = wholeWord,
                            onCheckedChange = {
                                wholeWord = it
                                if (findText.isNotEmpty()) {
                                    matchCount = onFind(findText, caseSensitive, wholeWord)
                                }
                            }
                        )
                        Text("Whole word", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                HorizontalDivider()
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showReplace = !showReplace }
                    ) {
                        Icon(
                            if (showReplace) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (showReplace) "Hide Replace" else "Show Replace")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showReplace) {
                            OutlinedButton(
                                onClick = {
                                    if (findText.isNotEmpty()) {
                                        val replaced = onReplace(findText, replaceText, caseSensitive, wholeWord)
                                        matchCount = replaced
                                    }
                                },
                                enabled = findText.isNotEmpty()
                            ) {
                                Text("Replace")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    if (findText.isNotEmpty()) {
                                        val replaced = onReplaceAll(findText, replaceText, caseSensitive, wholeWord)
                                        matchCount = 0
                                    }
                                },
                                enabled = findText.isNotEmpty()
                            ) {
                                Text("Replace All")
                            }
                        }
                        
                        Button(
                            onClick = {
                                if (findText.isNotEmpty()) {
                                    onFind(findText, caseSensitive, wholeWord)
                                }
                            },
                            enabled = findText.isNotEmpty()
                        ) {
                            Icon(Icons.Default.NavigateNext, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Find Next")
                        }
                    }
                }
            }
        }
    }
}
