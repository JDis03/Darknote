package com.darknote.core.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownParserTest {

    // ── Headers ─────────────────────────────────────────────────────────────

    @Test
    fun `atx headers of all levels`() {
        val blocks = MarkdownParser.parse("# H1\n### H3\n###### H6")
        assertEquals(3, blocks.size)
        assertEquals(MdBlock.Header(1, listOf(MdInline.Text("H1"))), blocks[0])
        assertEquals(MdBlock.Header(3, listOf(MdInline.Text("H3"))), blocks[1])
        assertEquals(MdBlock.Header(6, listOf(MdInline.Text("H6"))), blocks[2])
    }

    @Test
    fun `atx header with closing hashes is stripped`() {
        val blocks = MarkdownParser.parse("## Title ##")
        assertEquals(MdBlock.Header(2, listOf(MdInline.Text("Title"))), blocks.single())
    }

    @Test
    fun `hash without space is not a header`() {
        val blocks = MarkdownParser.parse("#notAHeader")
        assertIs<MdBlock.Paragraph>(blocks.single())
    }

    @Test
    fun `setext headers from underline`() {
        val blocks = MarkdownParser.parse("Big Title\n=====\nSmall Title\n-----")
        assertEquals(2, blocks.size)
        assertEquals(MdBlock.Header(1, listOf(MdInline.Text("Big Title"))), blocks[0])
        assertEquals(MdBlock.Header(2, listOf(MdInline.Text("Small Title"))), blocks[1])
    }

    // ── Paragraphs ──────────────────────────────────────────────────────────

    @Test
    fun `consecutive lines merge into one paragraph, blank line splits`() {
        val blocks = MarkdownParser.parse("line one\nline two\n\nline three")
        assertEquals(2, blocks.size)
        val p1 = assertIs<MdBlock.Paragraph>(blocks[0])
        assertEquals(listOf(MdInline.Text("line one\nline two")), p1.spans)
        val p2 = assertIs<MdBlock.Paragraph>(blocks[1])
        assertEquals(listOf(MdInline.Text("line three")), p2.spans)
    }

    // ── Code blocks ─────────────────────────────────────────────────────────

    @Test
    fun `fenced code block with language`() {
        val blocks = MarkdownParser.parse("```kotlin\nval x = 1\nval y = 2\n```")
        assertEquals(
            MdBlock.CodeBlock("val x = 1\nval y = 2", "kotlin"),
            blocks.single()
        )
    }

    @Test
    fun `tilde fences work too`() {
        val blocks = MarkdownParser.parse("~~~py\nprint(1)\n~~~")
        assertEquals(MdBlock.CodeBlock("print(1)", "py"), blocks.single())
    }

    @Test
    fun `unterminated fence consumes to end of document without throwing`() {
        val blocks = MarkdownParser.parse("```\nval x = 1\n# not a header")
        val code = assertIs<MdBlock.CodeBlock>(blocks.single())
        assertEquals("val x = 1\n# not a header", code.code)
    }

    @Test
    fun `hash and emphasis inside code block stay literal`() {
        val blocks = MarkdownParser.parse("```\n# comment **not bold**\n```")
        val code = assertIs<MdBlock.CodeBlock>(blocks.single())
        assertEquals("# comment **not bold**", code.code)
    }

    // ── Block quotes ────────────────────────────────────────────────────────

    @Test
    fun `nested block quotes track depth`() {
        val blocks = MarkdownParser.parse("> outer\n>> inner\n>>> deepest")
        assertEquals(3, blocks.size)
        assertEquals(1, assertIs<MdBlock.BlockQuote>(blocks[0]).depth)
        assertEquals(2, assertIs<MdBlock.BlockQuote>(blocks[1]).depth)
        assertEquals(3, assertIs<MdBlock.BlockQuote>(blocks[2]).depth)
        assertEquals(
            listOf(MdInline.Text("deepest")),
            assertIs<MdBlock.BlockQuote>(blocks[2]).spans
        )
    }

    // ── Lists ───────────────────────────────────────────────────────────────

    @Test
    fun `unordered list items`() {
        val blocks = MarkdownParser.parse("- one\n* two\n+ three")
        assertEquals(3, blocks.size)
        blocks.forEach {
            val item = assertIs<MdBlock.ListItem>(it)
            assertTrue(!item.ordered)
            assertNull(item.taskChecked)
            assertEquals(0, item.depth)
        }
    }

    @Test
    fun `ordered list items keep their numbers`() {
        val blocks = MarkdownParser.parse("1. first\n2. second\n10. tenth")
        val numbers = blocks.map { assertIs<MdBlock.ListItem>(it).number }
        assertEquals(listOf(1, 2, 10), numbers)
    }

    @Test
    fun `task list checkboxes parsed`() {
        val blocks = MarkdownParser.parse("- [ ] todo\n- [x] done\n- [X] also done")
        val checks = blocks.map { assertIs<MdBlock.ListItem>(it).taskChecked }
        assertEquals(listOf(false, true, true), checks)
        assertEquals(
            listOf(MdInline.Text("todo")),
            assertIs<MdBlock.ListItem>(blocks[0]).spans
        )
    }

    @Test
    fun `nested list indentation maps to depth`() {
        val blocks = MarkdownParser.parse("- top\n    - nested\n        - deeper")
        val depths = blocks.map { assertIs<MdBlock.ListItem>(it).depth }
        assertEquals(listOf(0, 1, 2), depths)
    }

    // ── Horizontal rules ────────────────────────────────────────────────────

    @Test
    fun `horizontal rule variants`() {
        val blocks = MarkdownParser.parse("---\n***\n_ _ _\n")
        blocks.forEach { assertIs<MdBlock.HorizontalRule>(it) }
        assertEquals(3, blocks.size)
    }

    // ── Inline: emphasis ────────────────────────────────────────────────────

    @Test
    fun `bold italic and bold-italic`() {
        val spans = MarkdownParser.parseInline("**b** *i* ***bi*** __u__ _e_")
        assertIs<MdInline.Bold>(spans[0])
        assertIs<MdInline.Italic>(spans[2])
        assertIs<MdInline.BoldItalic>(spans[4])
        assertIs<MdInline.Bold>(spans[6])
        assertIs<MdInline.Italic>(spans[8])
    }

    @Test
    fun `nested emphasis inside bold`() {
        val spans = MarkdownParser.parseInline("**bold and *italic* end**")
        val bold = assertIs<MdInline.Bold>(spans.single())
        assertEquals(MdInline.Text("bold and "), bold.children[0])
        assertIs<MdInline.Italic>(bold.children[1])
        assertEquals(MdInline.Text(" end"), bold.children[2])
    }

    @Test
    fun `unmatched emphasis is literal — half-typed bold never breaks`() {
        val spans = MarkdownParser.parseInline("**incomplete bold")
        assertEquals(listOf(MdInline.Text("**incomplete bold")), spans)
    }

    @Test
    fun `strikethrough and highlight`() {
        val spans = MarkdownParser.parseInline("~~gone~~ ==marked==")
        assertIs<MdInline.Strikethrough>(spans[0])
        assertIs<MdInline.Highlight>(spans[2])
    }

    // ── Inline: code ────────────────────────────────────────────────────────

    @Test
    fun `inline code span`() {
        val spans = MarkdownParser.parseInline("use `val x = 1` here")
        val code = spans.filterIsInstance<MdInline.Code>()
        assertEquals(listOf(MdInline.Code("val x = 1")), code)
    }

    @Test
    fun `double backtick span can contain a single backtick`() {
        val spans = MarkdownParser.parseInline("``a`b``")
        assertEquals(listOf(MdInline.Code("a`b")), spans)
    }

    @Test
    fun `unclosed backtick is literal`() {
        val spans = MarkdownParser.parseInline("`oops")
        assertEquals(listOf(MdInline.Text("`oops")), spans)
    }

    // ── Inline: links & images ──────────────────────────────────────────────

    @Test
    fun `link with href`() {
        val spans = MarkdownParser.parseInline("see [the docs](https://example.com) now")
        val link = spans.filterIsInstance<MdInline.Link>().single()
        assertEquals("https://example.com", link.href)
        assertEquals(listOf(MdInline.Text("the docs")), link.text)
    }

    @Test
    fun `link text can contain emphasis`() {
        val spans = MarkdownParser.parseInline("[**bold** link](https://x.com)")
        val link = assertIs<MdInline.Link>(spans.single())
        assertEquals(2, link.text.size)
        assertIs<MdInline.Bold>(link.text[0])
        assertEquals(MdInline.Text(" link"), link.text[1])
    }

    @Test
    fun `autolink in angle brackets`() {
        val spans = MarkdownParser.parseInline("<https://example.com>")
        val link = assertIs<MdInline.Link>(spans.single())
        assertEquals("https://example.com", link.href)
    }

    @Test
    fun `image with alt and src`() {
        val spans = MarkdownParser.parseInline("![diagram](img.png)")
        assertEquals(listOf(MdInline.Image("diagram", "img.png")), spans)
    }

    // ── Inline: escapes ─────────────────────────────────────────────────────

    @Test
    fun `backslash escapes make specials literal`() {
        val spans = MarkdownParser.parseInline("\\*not italic\\* \\# not header")
        assertEquals(
            listOf(MdInline.Text("*not italic* # not header")),
            spans
        )
    }

    // ── Robustness: the "paste anything" contract ───────────────────────────

    @Test
    fun `malformed mixed input never throws and produces blocks`() {
        val nasty = buildString {
            append("# ok\n\n")
            append("```\nunclosed fence\n**bold in code\n\n")
            append(">> deep quote **unclosed\n")
            append("- [ broken task\n")
            append("[unclosed link](\n")
            append("** ** **")
        }
        val blocks = MarkdownParser.parse(nasty)
        assertTrue(blocks.isNotEmpty())
        // The unclosed fence must have swallowed its content as code, not headers
        assertTrue(blocks.any { it is MdBlock.CodeBlock })
    }

    @Test
    fun `empty document parses to empty list`() {
        assertEquals(emptyList(), MarkdownParser.parse(""))
        assertEquals(emptyList(), MarkdownParser.parse("\n\n\n"))
    }

    @Test
    fun `windows line endings handled`() {
        val blocks = MarkdownParser.parse("# Title\r\n\r\n- item\r\n")
        assertEquals(2, blocks.size)
        assertIs<MdBlock.Header>(blocks[0])
        assertIs<MdBlock.ListItem>(blocks[1])
    }
}
