package com.darknote.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darknote.desktop.editor.highlighter.SyntaxHighlightTransformation
import com.darknote.desktop.editor.highlighter.SyntaxHighlighterFactory
import com.darknote.desktop.editor.theme.EditorColorScheme

/**
 * Advanced code editor component with line numbers and syntax highlighting.
 * Similar to Kate/KWrite editor.
 * 
 * Features:
 * - Line numbers (optional)
 * - Syntax highlighting for multiple languages
 * - Read-only mode
 * - Monospace font
 * - Customizable color scheme
 * 
 * @param value Current text content
 * @param onValueChange Callback when text changes
 * @param language Programming language for syntax highlighting (e.g., "kotlin", "python")
 * @param modifier Modifier for the editor
 * @param readOnly Whether the editor is read-only (default: false)
 * @param showLineNumbers Whether to show line numbers (default: true)
 * @param colorScheme Color scheme for syntax highlighting (defaults to dark theme)
 */
@Composable
fun CodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    language: String? = null,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    showLineNumbers: Boolean = true,
    colorScheme: EditorColorScheme = EditorColorScheme.DARK
) {
    val scrollState = rememberScrollState()
    val lines = remember(value) { value.lines() }
    val lineCount = lines.size
    
    // Create syntax highlighter for the given language
    val highlighter = remember(language) {
        SyntaxHighlighterFactory.create(language)
    }
    
    // Highlight the text (memoized to avoid re-highlighting on every recomposition)
    val highlightedText = remember(value, language, colorScheme) {
        highlighter.highlight(value, colorScheme)
    }
    
    Row(modifier = modifier) {
        // Line numbers column
        if (showLineNumbers) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .verticalScroll(scrollState, enabled = false) // Synced scroll
            ) {
                repeat(lineCount) { index ->
                    Text(
                        text = "${index + 1}",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.height(22.dp)
                    )
                }
            }
        }
        
        // Editor content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (readOnly) {
                // Read-only view with syntax highlighting
                SelectionContainer {
                    Text(
                        text = highlightedText,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            } else {
                // Editable text field
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = SyntaxHighlightTransformation(language, colorScheme)
                )
            }
        }
    }
}
