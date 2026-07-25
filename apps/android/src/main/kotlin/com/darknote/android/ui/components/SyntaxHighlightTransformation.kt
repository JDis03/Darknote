package com.darknote.android.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * VisualTransformation that applies live syntax highlighting to a BasicTextField
 * using the existing [SyntaxHighlighter].
 *
 * The offset mapping is identity: no characters are added or removed, only
 * colors/styles are layered on top for display. This means:
 * - The underlying raw `String`/`TextFieldValue.text` is never touched.
 * - Cursor position and text selection stay perfectly in sync with the raw text.
 * - Pasting content (rich or plain) is unaffected — paste is handled by the
 *   platform's default BasicTextField behavior *before* this transformation
 *   ever runs; this class only recolors whatever text already landed in state.
 *
 * Mirrors apps/desktop's SyntaxHighlightTransformation for parity between platforms.
 */
class SyntaxHighlightTransformation(
    private val language: String?
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(text.text, language)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
