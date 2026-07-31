package com.darknote.android.test

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import com.darknote.android.ui.components.MarkdownLiveStyle
import com.darknote.android.ui.components.MarkdownVisualTransformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The whole point of MarkdownVisualTransformation (vs. the earlier WYSIWYG
 * library approach) is that it NEVER alters a single character of the raw
 * text — it only recolors/resizes the same string via identity offset
 * mapping. These tests pin that guarantee down for the exact real-world
 * input that surfaced the original bug (a pasted blockquote getting
 * silently reformatted), plus a battery of other markdown constructs and
 * edge cases (empty string, unmatched markers, nested/adjacent constructs).
 */
class MarkdownVisualTransformationTest {

    private fun assertTextUnchanged(input: String) {
        val styled = MarkdownLiveStyle.style(input)
        assertEquals(input, styled.text, "MarkdownLiveStyle must never alter the raw text")

        val transformed = MarkdownVisualTransformation().filter(AnnotatedString(input))
        assertEquals(input, transformed.text.text, "VisualTransformation output text must match input")
        assertEquals(
            OffsetMapping.Identity,
            transformed.offsetMapping,
            "Must use identity offset mapping — no characters may be added/removed/reordered"
        )
    }

    @Test
    fun `the exact blockquote the user pasted is preserved byte for byte`() {
        val input = """
            > #### The quarterly results look great!
            >
            > - Revenue was off the chart.
            > - Profits were higher than ever.
            >
            >  *Everything* is going according to **plan**.
        """.trimIndent()
        assertTextUnchanged(input)
    }

    @Test
    fun `headers of all levels are preserved`() {
        assertTextUnchanged("# H1\n## H2\n### H3\n#### H4\n##### H5\n###### H6\nBody text")
    }

    @Test
    fun `bold italic strikethrough and inline code are preserved`() {
        assertTextUnchanged("This is **bold**, *italic*, ~~struck~~, and `code`.")
    }

    @Test
    fun `mixed bold and italic in the same line are preserved`() {
        assertTextUnchanged("**bold** then *italic* then **bold again** and _italic again_")
    }

    @Test
    fun `lists ordered and unordered are preserved`() {
        assertTextUnchanged("- one\n- two\n* three\n1. first\n2. second")
    }

    @Test
    fun `links are preserved`() {
        assertTextUnchanged("Check [this link](https://example.com/path?q=1) out.")
    }

    @Test
    fun `fenced code block is preserved`() {
        assertTextUnchanged("```kotlin\nval x = 1\nprintln(x)\n```")
    }

    @Test
    fun `fenced python code block from user report gets monospace and visible background`() {
        // Exact input reported: "I pasted ```python\nprint("Hola")\n``` and saw nothing".
        // The old implementation used a nearly-invisible 12%-opacity green background
        // and applied the same colour to fences + code content alike — indistinguishable
        // from surrounding text. The fix splits fences (dimmed) from code content
        // (monospace + visible ~15% grey background), matching Obsidian's treatment.
        val input = "```python\nprint(\"Hola\")\n```"
        assertTextUnchanged(input)

        val styled = MarkdownLiveStyle.style(input)

        // Opening fence (```python\n) should be dimmed
        val openFenceStyle = styled.spanStyles.find { it.start == 0 && it.end == 10 }
        assertTrue(openFenceStyle != null, "Opening fence (0..10) must have a dimmed style applied")

        // Code content should have monospace AND a visible background
        val codeContentStyle = styled.spanStyles.find { it.start == 10 }
        assertTrue(codeContentStyle != null, "Code content (starting at 10) must have monospace+background style")
        assertEquals(FontFamily.Monospace, codeContentStyle!!.item.fontFamily)
        assertTrue(codeContentStyle.item.background != null && codeContentStyle.item.background != androidx.compose.ui.graphics.Color.Transparent,
            "Code content must have a visible background — the old 0x1F-almost-invisible background was the bug")
    }

    @Test
    fun `horizontal rules are preserved`() {
        assertTextUnchanged("Above\n\n---\n\nBelow\n\n***\n\nEnd")
    }

    @Test
    fun `nested blockquote levels are preserved`() {
        assertTextUnchanged(">> deeply nested quote\n> single level")
    }

    @Test
    fun `unmatched or malformed markers do not throw and text is preserved`() {
        // Dangling markers, empty link text/url, unbalanced asterisks etc. — none
        // of these should throw (e.g. from an invalid/empty regex range) or alter text.
        assertTextUnchanged("* not a list marker without a following space*")
        assertTextUnchanged("[]()")
        assertTextUnchanged("**unterminated bold")
        assertTextUnchanged("> ")
        assertTextUnchanged("#")
        assertTextUnchanged("")
    }

    @Test
    fun `plain text with no markdown constructs is preserved`() {
        assertTextUnchanged("Just a normal sentence with no special characters at all.")
    }

    @Test
    fun `repeated styling calls are idempotent in output text`() {
        val input = "# Title\n\n> quote\n\n- item **bold** *italic*"
        val first = MarkdownLiveStyle.style(input).text
        val second = MarkdownLiveStyle.style(first).text
        assertEquals(input, first)
        assertEquals(first, second)
    }
}
