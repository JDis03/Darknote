package com.darknote.desktop.editor.highlighter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.darknote.desktop.editor.theme.EditorColorScheme

/**
 * VisualTransformation that applies syntax highlighting to a BasicTextField.
 *
 * Uses the existing SyntaxHighlighterFactory to convert raw text into an
 * AnnotatedString with syntax-colored spans. The offset mapping is identity
 * because no characters are added or removed — only colors are applied.
 *
 * Example usage:
 * ```kotlin
 * BasicTextField(
 *     value = code,
 *     onValueChange = { code = it },
 *     visualTransformation = SyntaxHighlightTransformation("kotlin")
 * )
 * ```
 *
 * @param language Language identifier passed to SyntaxHighlighterFactory (nullable for plain text)
 * @param colorScheme Color scheme for syntax tokens (defaults to DARK)
 */
class SyntaxHighlightTransformation(
    private val language: String?,
    private val colorScheme: EditorColorScheme = EditorColorScheme.DARK
) : VisualTransformation {

    private val highlighter = SyntaxHighlighterFactory.create(language)

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = highlighter.highlight(text.text, colorScheme)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
