package com.darknote.android.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Live markdown display styling for the eye-mode editor.
 *
 * This is a DECORATION-ONLY styler: it never adds, removes, or reorders a
 * single character. It only recolors/resizes/re-weights the *same* raw text
 * the BasicTextField holds, via [VisualTransformation] with an identity
 * offset mapping (same approach as [SyntaxHighlightTransformation]).
 *
 * This is a deliberate architectural choice: an earlier version of this
 * feature used a WYSIWYG library (richeditor-compose) that parses the text
 * into an internal tree and re-serializes it back to markdown on every edit.
 * That re-serialization is NOT a diff — it regenerates the ENTIRE document
 * from the tree, which silently rewrites the syntax of untouched parts of
 * the note (e.g. a blockquote you never touched) the moment you edit
 * anything else, because the library's own markdown-writing conventions
 * don't necessarily match what you originally typed. That's unfixable
 * within a parse-and-regenerate architecture.
 *
 * Styling the raw text in place instead — the same technique Obsidian's and
 * Joplin's "live preview" editors use under the hood — guarantees byte-for-
 * byte preservation: what you type or paste is exactly what gets saved,
 * always, because contentField.text is never replaced by anything derived
 * from it. Only markdown MARKER characters (#, *, _, `, >, -, [ ], ( )) are
 * dimmed and heading/emphasis TEXT is styled; nothing is hidden or removed.
 */
object MarkdownLiveStyle {

    private val markerColor = Color(0xFF9AA0A6)
    private val headerColor = Color(0xFF4FA6FF)
    private val linkColor = Color(0xFFFFA726)
    private val urlColor = Color(0xFF9AA0A6)
    private val codeColor = Color(0xFF66BB6A)
    private val codeBackground = Color(0x1F66BB6A)
    private val quoteColor = Color(0xFF9E9E9E)
    private val listColor = Color(0xFFAB47BC)

    private val headerSizes = mapOf(
        1 to 26.sp, 2 to 23.sp, 3 to 20.sp, 4 to 18.sp, 5 to 16.sp, 6 to 15.sp
    )

    private val headerRule = Regex("(?m)^(#{1,6})([ \\t]+)(.*)$")
    private val fencedCodeRule = Regex("(?s)```[^\\n]*\\n?.*?```")
    private val inlineCodeRule = Regex("`[^`\\n]+`")
    private val boldRule = Regex("(\\*\\*[^*\\n]+\\*\\*)|(__[^_\\n]+__)")
    private val italicRule = Regex("(?<!\\*)\\*(?!\\*)[^*\\n]+\\*(?!\\*)|(?<!_)_(?!_)[^_\\n]+_(?!_)")
    private val strikethroughRule = Regex("~~[^~\\n]+~~")
    private val listMarkerRule = Regex("(?m)^\\s*([-*+]|\\d+\\.)\\s")
    private val blockQuoteRule = Regex("(?m)^(\\s*>+)(.*)$")
    private val linkRule = Regex("\\[([^\\]\\n]*)\\]\\(([^)\\n]*)\\)")
    private val horizontalRuleRule = Regex("(?m)^(-{3,}|\\*{3,}|_{3,})\\s*$")

    fun style(text: String): AnnotatedString = buildAnnotatedString {
        append(text)
        if (text.isEmpty()) return@buildAnnotatedString
        val len = text.length

        fun mark(style: SpanStyle, start: Int, end: Int) {
            val s = start.coerceIn(0, len)
            val e = end.coerceIn(0, len)
            if (e > s) addStyle(style, s, e)
        }

        // Headers: dim the "### " prefix, bold + scale + color the heading text.
        for (m in headerRule.findAll(text)) {
            val hashes = m.groupValues[1]
            val markerEnd = m.range.first + hashes.length + m.groupValues[2].length
            mark(SpanStyle(color = markerColor), m.range.first, markerEnd)
            mark(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = headerColor,
                    fontSize = headerSizes[hashes.length] ?: 15.sp
                ),
                markerEnd, m.range.last + 1
            )
        }

        // Fenced code blocks — dim fences implicitly via monospace/background block.
        for (m in fencedCodeRule.findAll(text)) {
            mark(
                SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor, background = codeBackground),
                m.range.first, m.range.last + 1
            )
        }

        // Inline code `code`
        for (m in inlineCodeRule.findAll(text)) {
            mark(SpanStyle(color = markerColor), m.range.first, m.range.first + 1)
            mark(
                SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor, background = codeBackground),
                m.range.first + 1, m.range.last
            )
            mark(SpanStyle(color = markerColor), m.range.last, m.range.last + 1)
        }

        // Bold **text** / __text__
        for (m in boldRule.findAll(text)) {
            val r = m.range
            mark(SpanStyle(color = markerColor), r.first, r.first + 2)
            mark(SpanStyle(fontWeight = FontWeight.Bold), r.first + 2, r.last - 1)
            mark(SpanStyle(color = markerColor), r.last - 1, r.last + 1)
        }

        // Italic *text* / _text_ (bold spans above take precedence where they overlap)
        for (m in italicRule.findAll(text)) {
            val r = m.range
            mark(SpanStyle(color = markerColor), r.first, r.first + 1)
            mark(SpanStyle(fontStyle = FontStyle.Italic), r.first + 1, r.last)
            mark(SpanStyle(color = markerColor), r.last, r.last + 1)
        }

        // Strikethrough ~~text~~
        for (m in strikethroughRule.findAll(text)) {
            val r = m.range
            mark(SpanStyle(color = markerColor), r.first, r.first + 2)
            mark(SpanStyle(textDecoration = TextDecoration.LineThrough), r.first + 2, r.last - 1)
            mark(SpanStyle(color = markerColor), r.last - 1, r.last + 1)
        }

        // List markers: -, *, +, or "1." at line start
        for (m in listMarkerRule.findAll(text)) {
            mark(SpanStyle(fontWeight = FontWeight.Bold, color = listColor), m.range.first, m.range.last + 1)
        }

        // Blockquotes: dim the ">" marker(s), italicize + mute the quoted text
        for (m in blockQuoteRule.findAll(text)) {
            val markerGroup = m.groups[1] ?: continue
            mark(SpanStyle(color = markerColor, fontWeight = FontWeight.Bold), markerGroup.range.first, markerGroup.range.last + 1)
            mark(SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor), markerGroup.range.last + 1, m.range.last + 1)
        }

        // Links [text](url) — dim brackets/parens and the URL, color the link text
        for (m in linkRule.findAll(text)) {
            val textGroup = m.groups[1]
            val urlGroup = m.groups[2]
            if (textGroup == null || urlGroup == null) continue
            mark(SpanStyle(color = markerColor), m.range.first, textGroup.range.first)
            mark(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), textGroup.range.first, textGroup.range.last + 1)
            mark(SpanStyle(color = markerColor), textGroup.range.last + 1, urlGroup.range.first)
            mark(SpanStyle(color = urlColor), urlGroup.range.first, urlGroup.range.last + 1)
            mark(SpanStyle(color = markerColor), urlGroup.range.last + 1, m.range.last + 1)
        }

        // Horizontal rules: ---, ***, ___
        for (m in horizontalRuleRule.findAll(text)) {
            mark(SpanStyle(color = markerColor), m.range.first, m.range.last + 1)
        }
    }
}

/**
 * [VisualTransformation] wrapper around [MarkdownLiveStyle.style]. Identity
 * offset mapping — see [MarkdownLiveStyle] doc for why that matters here.
 */
class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(MarkdownLiveStyle.style(text.text), OffsetMapping.Identity)
    }
}
