package com.darknote.desktop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Toolbar for code editor with common actions.
 * Similar to Kate's toolbar.
 */
@Composable
fun EditorToolbar(
    language: String?,
    onLanguageChange: (String?) -> Unit,
    wordWrap: Boolean,
    onWordWrapToggle: () -> Unit,
    showLineNumbers: Boolean,
    onLineNumbersToggle: () -> Unit,
    onFind: () -> Unit,
    onFormat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language selector
            var showLanguageMenu by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { showLanguageMenu = true },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Code, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(language ?: "Plain Text", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                }
                
                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false }
                ) {
                    LanguageMenuItem("Plain Text", null, language, onLanguageChange) { showLanguageMenu = false }
                    Divider()
                    LanguageMenuItem("Kotlin", "kotlin", language, onLanguageChange) { showLanguageMenu = false }
                    LanguageMenuItem("Java", "java", language, onLanguageChange) { showLanguageMenu = false }
                    LanguageMenuItem("Python", "python", language, onLanguageChange) { showLanguageMenu = false }
                    LanguageMenuItem("JavaScript", "javascript", language, onLanguageChange) { showLanguageMenu = false }
                    LanguageMenuItem("TypeScript", "typescript", language, onLanguageChange) { showLanguageMenu = false }
                    LanguageMenuItem("Rust", "rust", language, onLanguageChange) { showLanguageMenu = false }
                    LanguageMenuItem("Go", "go", language, onLanguageChange) { showLanguageMenu = false }
                    LanguageMenuItem("C/C++", "cpp", language, onLanguageChange) { showLanguageMenu = false }
                    LanguageMenuItem("SQL", "sql", language, onLanguageChange) { showLanguageMenu = false }
                    LanguageMenuItem("JSON", "json", language, onLanguageChange) { showLanguageMenu = false }
                    LanguageMenuItem("XML/HTML", "xml", language, onLanguageChange) { showLanguageMenu = false }
                }
            }
            
            VerticalDivider(modifier = Modifier.height(24.dp))
            
            // Find
            IconButton(
                onClick = onFind,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Search, "Find", modifier = Modifier.size(20.dp))
            }
            
            // Format code
            IconButton(
                onClick = onFormat,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.FormatAlignLeft, "Format", modifier = Modifier.size(20.dp))
            }
            
            VerticalDivider(modifier = Modifier.height(24.dp))
            
            // Word wrap toggle
            IconToggleButton(
                checked = wordWrap,
                onCheckedChange = { onWordWrapToggle() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.WrapText,
                    "Word Wrap",
                    modifier = Modifier.size(20.dp),
                    tint = if (wordWrap) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Line numbers toggle
            IconToggleButton(
                checked = showLineNumbers,
                onCheckedChange = { onLineNumbersToggle() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.FormatListNumbered,
                    "Line Numbers",
                    modifier = Modifier.size(20.dp),
                    tint = if (showLineNumbers) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.weight(1f))
            
            // Info/help
            IconButton(
                onClick = { /* Show keyboard shortcuts */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Info, "Help", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun LanguageMenuItem(
    label: String,
    value: String?,
    current: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            onSelect(value)
            onDismiss()
        },
        leadingIcon = if (current == value) {
            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
        } else null
    )
}
