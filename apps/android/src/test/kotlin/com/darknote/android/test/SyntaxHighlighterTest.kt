package com.darknote.android.test

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import com.darknote.android.ui.components.SyntaxHighlightTransformation
import com.darknote.android.ui.components.SyntaxHighlighter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers two things the manual audit flagged as risk areas:
 *
 * 1. Markdown is now a recognized language (parity with Desktop's MarkdownHighlighter),
 *    and its highlighting rules match the expected constructs (headers, code fences,
 *    bold/italic, lists, links, blockquotes) without throwing on edge cases.
 *
 * 2. The live editor's VisualTransformation can NEVER corrupt the raw text — this is
 *    what protects copy/paste and typing from any formatting side effects. It must
 *    always be an identity offset mapping and must always preserve `text.text` exactly
 *    in its output, no matter what garbage/rich/mixed content is pasted in.
 */
class SyntaxHighlighterTest {

    // ── Markdown grammar ─────────────────────────────────────────────────────

    @Test
    fun `markdown header is styled without altering text`() {
        val input = "# Title\nSome body text"
        val result = SyntaxHighlighter.highlight(input, "markdown")
        assertEquals(input, result.text)
    }

    @Test
    fun `markdown code fence does not break on unterminated block`() {
        // Regression guard: malformed/partial markdown (e.g. mid-paste) must not throw.
        val input = "```kotlin\nval x = 1"
        val result = SyntaxHighlighter.highlight(input, "markdown")
        assertEquals(input, result.text)
    }

    @Test
    fun `markdown bold italic list and link constructs preserve raw text`() {
        val input = """
            # Header
            **bold** and _italic_ and `inline code`
            - item one
            - item two
            > a quote
            [link](https://example.com)
        """.trimIndent()
        val result = SyntaxHighlighter.highlight(input, "markdown")
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "expected at least one styled span for markdown constructs")
    }

    @Test
    fun `md extension normalizes to markdown grammar`() {
        val input = "# Title"
        val viaAlias = SyntaxHighlighter.highlight(input, "md")
        val viaFull = SyntaxHighlighter.highlight(input, "markdown")
        assertEquals(viaFull.spanStyles.size, viaAlias.spanStyles.size)
    }

    @Test
    fun `unknown language falls back to plain text unchanged`() {
        val input = "whatever content"
        val result = SyntaxHighlighter.highlight(input, "not-a-real-language")
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    // ── VisualTransformation safety (copy/paste protection) ─────────────────

    @Test
    fun `transformation never mutates raw text for any language`() {
        val samples = listOf(
            "plain text, no language" to null,
            "def f():\n    return 1" to "python",
            "# Markdown\n**bold** `code`" to "markdown",
            "SELECT * FROM t WHERE x = 1;" to "sql",
            "" to "kotlin",
            "weird \u0000 control \uD83D\uDE00 emoji and\ttabs" to "bash"
        )

        for ((text, lang) in samples) {
            val transformation = SyntaxHighlightTransformation(lang)
            val transformed: TransformedText = transformation.filter(AnnotatedString(text))
            assertEquals(text, transformed.text.text, "raw text must survive transformation for language=$lang")
            assertEquals(OffsetMapping.Identity, transformed.offsetMapping,
                "offset mapping must be Identity so cursor/selection/paste offsets are never desynced")
        }
    }

    @Test
    fun `pasting content mid-document keeps cursor-relevant offsets untouched`() {
        // Simulates: user pastes a block of code into the middle of existing content.
        // Because offsetMapping is Identity, transformedOffset == originalOffset always.
        val original = "before "
        val pasted = "PASTED_BLOCK\nline2"
        val after = " after"
        val fullText = original + pasted + after

        val transformation = SyntaxHighlightTransformation("kotlin")
        val transformed = transformation.filter(AnnotatedString(fullText))

        assertEquals(fullText, transformed.text.text)
        val pasteEndOffset = (original + pasted).length
        assertEquals(pasteEndOffset, transformed.offsetMapping.originalToTransformed(pasteEndOffset))
        assertEquals(pasteEndOffset, transformed.offsetMapping.transformedToOriginal(pasteEndOffset))
    }
}
