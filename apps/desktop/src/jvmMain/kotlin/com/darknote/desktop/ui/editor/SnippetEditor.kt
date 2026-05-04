package com.darknote.desktop.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.darknote.core.model.Snippet

enum class SaveStatus { Idle, Saving, Saved, Error }

@Composable
fun SnippetEditor(
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
    if (snippet == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Select a snippet to edit",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar
        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (saveStatus) {
                        SaveStatus.Saving -> "Saving..."
                        SaveStatus.Saved -> "Saved"
                        SaveStatus.Error -> "Error saving"
                        SaveStatus.Idle -> if (isModified) "Modified" else ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (saveStatus) {
                        SaveStatus.Saved -> MaterialTheme.colorScheme.primary
                        SaveStatus.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(Modifier.width(8.dp))

                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onCopyRaw, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, "Copy raw", modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onSave,
                    enabled = isModified,
                    modifier = Modifier.size(32.dp)
                ) {
                     Icon(Icons.Filled.SaveAs, "Save", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Editor
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .weight(1f)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        )
    }
}
