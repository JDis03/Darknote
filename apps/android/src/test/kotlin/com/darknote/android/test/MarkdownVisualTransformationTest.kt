package com.darknote.android.test

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import com.darknote.android.ui.components.MarkdownLiveStyle
import com.darknote.android.ui.components.MarkdownVisualTransformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownVisualTransformationTest {

    private fun assertTextUnchanged(input: String) {
        val styled = MarkdownLiveStyle.style(input)
        assertEquals(input, styled.text, "MarkdownLiveStyle must never alter the raw text")

        val transformed = MarkdownVisualTransformation().filter(AnnotatedString(input))
        assertEquals(input, transformed.text.text, "VisualTransformation output text must match input")
        assertEquals(
            OffsetMapping.Identity,
            transformed.offsetMapping,
            "Must use identity offset mapping"
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
    fun `fenced python code block gets monospace and visible background`() {
        val input = "```python\nprint(\"Hola\")\n```"
        assertTextUnchanged(input)

        val styled = MarkdownLiveStyle.style(input)

        val openFenceStyle = styled.spanStyles.find { it.start == 0 && it.end == 10 }
        assertTrue(openFenceStyle != null, "Opening fence (0..10) must have a dimmed style applied")

        val codeContentStyle = styled.spanStyles.find { it.start == 10 }
        assertTrue(codeContentStyle != null, "Code content (starting at 10) must have monospace+background style")
        assertEquals(FontFamily.Monospace, codeContentStyle!!.item.fontFamily)
        assertTrue(
            codeContentStyle.item.background != null &&
                codeContentStyle.item.background != androidx.compose.ui.graphics.Color.Transparent,
            "Code content must have a visible background"
        )
    }

    @Test
    fun `fenced code with Windows line endings gets styled`() {
        val input = "```python\r\nprint(\"Hola\")\r\n```"
        assertTextUnchanged(input)

        val styled = MarkdownLiveStyle.style(input)
        val hasMonospace = styled.spanStyles.any { range ->
            range.item.fontFamily == FontFamily.Monospace &&
                range.item.background != null &&
                range.item.background != androidx.compose.ui.graphics.Color.Transparent
        }
        assertTrue(hasMonospace, "CRLF code block must get monospace + background styling")
    }

    @Test
    fun `debug - valid markdown produces at least one style span`() {
        val input = "# Hi\n\n```kotlin\nval x = 1\n```\n\nBody **bold**"
        val styled = MarkdownLiveStyle.style(input)
        assertTrue(styled.spanStyles.isNotEmpty(),
            "Expected at least one style span for '# Hi', code fence, and bold text. " +
            "Got 0 spans — regexes are not matching anything!")
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
