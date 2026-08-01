package com.darknote.core.markdown

/**
 * Serializes [MdBlock]/[MdInline] values (produced by [MarkdownParser])
 * back to raw Markdown text.
 *
 * Used for "copy this block" actions in the preview UI: the user taps a
 * copy button on a single rendered block and gets its markdown source on
 * the clipboard, without needing to select/copy from the raw text editor.
 *
 * This is NOT a round-trip guarantee for the whole document — see the
 * MarkdownLiveStyle / richeditor-compose history in this project for why
 * full-document AST round-tripping is unsafe as an editing mechanism. This
 * serializer is only ever used to produce a NEW string for the clipboard
 * from a block the user explicitly asked to copy; it never writes back
 * into the editor's source of truth, so any minor normalization (e.g.
 * always emitting `_italic_` regardless of whether the source used `*`)
 * is harmless here.
 */
object MarkdownSerializer {

    fun serialize(blocks: List<MdBlock>): String =
        blocks.joinToString("\n\n") { it.toMarkdown() }

    fun serialize(block: MdBlock): String = block.toMarkdown()
}

private fun MdBlock.toMarkdown(): String = when (this) {
    is MdBlock.Header -> "${"#".repeat(level)} ${spans.toMarkdown()}"
    is MdBlock.Paragraph -> spans.toMarkdown()
    is MdBlock.CodeBlock -> {
        val fence = "```"
        if (language != null) "$fence$language\n$code\n$fence"
        else "$fence\n$code\n$fence"
    }
    is MdBlock.BlockQuote -> {
        val prefix = "> ".repeat(depth).trimEnd()
        val lines = spans.toMarkdown().lines()
        lines.joinToString("\n") { "$prefix $it" }
    }
    is MdBlock.ListItem -> {
        val indent = "    ".repeat(depth)
        val marker = when {
            taskChecked == true -> "- [x] "
            taskChecked == false -> "- [ ] "
            ordered -> "${number ?: 1}. "
            else -> "- "
        }
        val lines = spans.toMarkdown().lines()
        lines.mapIndexed { idx, line ->
            if (idx == 0) "$indent$marker$line" else "$indent    $line"
        }.joinToString("\n")
    }
    MdBlock.HorizontalRule -> "---"
    is MdBlock.Table -> {
        val headerLine = headers.joinToString(" | ") { it.toMarkdown() }
        val sepLine = alignments.joinToString(" | ") { align ->
            when (align) {
                MdBlock.Table.ColumnAlignment.LEFT -> ":---"
                MdBlock.Table.ColumnAlignment.CENTER -> ":---:"
                MdBlock.Table.ColumnAlignment.RIGHT -> "---:"
                MdBlock.Table.ColumnAlignment.NONE -> "---"
            }
        }
        val rowLines = rows.joinToString("\n") { row ->
            "| ${row.joinToString(" | ") { it.toMarkdown() }} |"
        }
        buildString {
            append("| $headerLine |\n")
            append("| $sepLine |")
            if (rows.isNotEmpty()) {
                append("\n")
                append(rowLines)
            }
        }
    }
}

private fun List<MdInline>.toMarkdown(): String =
    joinToString("") { it.toMarkdown() }

private fun MdInline.toMarkdown(): String = when (this) {
    is MdInline.Text -> text
    is MdInline.Bold -> "**${children.toMarkdown()}**"
    is MdInline.Italic -> "_${children.toMarkdown()}_"
    is MdInline.BoldItalic -> "***${children.toMarkdown()}***"
    is MdInline.Strikethrough -> "~~${children.toMarkdown()}~~"
    is MdInline.Highlight -> "==${children.toMarkdown()}=="
    is MdInline.Code -> "`$text`"
    is MdInline.Link -> "[${text.toMarkdown()}]($href)"
    is MdInline.Image -> "![$alt]($src)"
}
