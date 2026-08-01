package com.darknote.core.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers MarkdownSerializer, used by the "copy this block" feature in the
 * preview UI. Two kinds of checks:
 *
 * 1. Exact string output for each block type (what actually lands on the
 *    clipboard).
 * 2. Semantic round-trip: parse(serialize(parse(input))) must equal
 *    parse(input) — re-parsing the serialized output must reconstruct an
 *    equivalent block, even if the exact source syntax differs (e.g. `*x*`
 *    serializes as `_x_` — same AST, different but valid markdown). This is
 *    NOT the same guarantee as the editor's source-of-truth text (which is
 *    never touched by this feature) — see MarkdownSerializer's doc comment.
 */
class MarkdownSerializerTest {

    private fun roundTripsSemantically(input: String) {
        val originalBlocks = MarkdownParser.parse(input)
        val serialized = MarkdownSerializer.serialize(originalBlocks)
        val reparsedBlocks = MarkdownParser.parse(serialized)
        assertEquals(originalBlocks, reparsedBlocks,
            "Re-parsing the serialized output must reconstruct an equivalent AST.\n" +
            "Serialized was:\n$serialized")
    }

    @Test
    fun `header serializes with correct hash count`() {
        val blocks = MarkdownParser.parse("### Title")
        assertEquals("### Title", MarkdownSerializer.serialize(blocks))
    }

    @Test
    fun `header levels 1 through 6 round-trip`() {
        roundTripsSemantically("# H1\n\n## H2\n\n### H3\n\n#### H4\n\n##### H5\n\n###### H6")
    }

    @Test
    fun `paragraph with bold and italic round-trips semantically`() {
        roundTripsSemantically("This is **bold** and *italic* text.")
    }

    @Test
    fun `code block preserves language and body exactly`() {
        val input = "```python\nprint(\"Hola\")\n```"
        val blocks = MarkdownParser.parse(input)
        assertEquals(input, MarkdownSerializer.serialize(blocks))
    }

    @Test
    fun `code block without language tag`() {
        val input = "```\nplain code\n```"
        val blocks = MarkdownParser.parse(input)
        assertEquals(input, MarkdownSerializer.serialize(blocks))
    }

    @Test
    fun `blockquote serializes with prefix`() {
        val blocks = MarkdownParser.parse("> Quoted text")
        assertEquals("> Quoted text", MarkdownSerializer.serialize(blocks))
    }

    @Test
    fun `unordered list item serializes with dash marker`() {
        val blocks = MarkdownParser.parse("- Item one")
        assertEquals("- Item one", MarkdownSerializer.serialize(blocks))
    }

    @Test
    fun `ordered list item serializes with its own number`() {
        val blocks = MarkdownParser.parse("3. Third item")
        assertEquals("3. Third item", MarkdownSerializer.serialize(blocks))
    }

    @Test
    fun `task list items serialize with checkbox markers`() {
        val checked = MarkdownParser.parse("- [x] Done")
        assertEquals("- [x] Done", MarkdownSerializer.serialize(checked))

        val unchecked = MarkdownParser.parse("- [ ] Todo")
        assertEquals("- [ ] Todo", MarkdownSerializer.serialize(unchecked))
    }

    @Test
    fun `horizontal rule serializes as triple dash`() {
        val blocks = MarkdownParser.parse("---")
        assertEquals("---", MarkdownSerializer.serialize(blocks))
    }

    @Test
    fun `single block serialize overload matches list overload for one block`() {
        val blocks = MarkdownParser.parse("# Only Block")
        val block = blocks.single()
        assertEquals(MarkdownSerializer.serialize(blocks), MarkdownSerializer.serialize(block))
    }

    @Test
    fun `link round-trips semantically`() {
        roundTripsSemantically("Check [this link](https://example.com/path?q=1) out.")
    }

    @Test
    fun `image round-trips semantically`() {
        roundTripsSemantically("![alt text](https://example.com/pic.png)")
    }

    @Test
    fun `strikethrough and inline code round-trip semantically`() {
        roundTripsSemantically("This is ~~struck~~ and `code`.")
    }

    @Test
    fun `nested bold inside italic round-trips semantically`() {
        roundTripsSemantically("This is *italic with **bold** inside*.")
    }

    @Test
    fun `multi-line blockquote (the earlier richeditor-compose bug case) round-trips semantically`() {
        // Same shape of document that surfaced the earlier richeditor-compose
        // reformatting bug — confirming the serializer handles it correctly as
        // an independent, explicit "copy" action. Unlike that earlier bug, this
        // never writes back into the editor's own contentField — it only ever
        // produces a fresh string for the clipboard.
        roundTripsSemantically(
            "> #### The quarterly results look great!\n" +
            "> - Revenue was off the chart.\n" +
            "> - Profits were higher than ever.\n" +
            "> *Everything* is going according to **plan**."
        )
    }

    @Test
    fun `empty document serializes to empty string`() {
        val blocks = MarkdownParser.parse("")
        assertEquals("", MarkdownSerializer.serialize(blocks))
    }
}
