package com.darknote.core.markdown

/**
 * Markdown document model produced by [MarkdownParser].
 *
 * The parser is a two-phase, stateful implementation:
 *  1. Block phase — walks the document line by line, tracking open/closed
 *     state for multi-line constructs (fenced code blocks, paragraphs,
 *     block-quote depth, nested list indentation).
 *  2. Inline phase — runs a recursive descent inline parser over each
 *     text-bearing block, handling backslash escapes, code spans, bold /
 *     italic / bold-italic, strikethrough, highlight, links and images.
 *
 * The block model below is platform-agnostic so both the Android and
 * Desktop preview UIs can render the same parsed document.
 */
sealed interface MdBlock {
    data class Header(val level: Int, val spans: List<MdInline>) : MdBlock
    data class Paragraph(val spans: List<MdInline>) : MdBlock
    data class CodeBlock(val code: String, val language: String?) : MdBlock
    /** [depth] starts at 1 for `> text`. */
    data class BlockQuote(val depth: Int, val spans: List<MdInline>) : MdBlock
    data class ListItem(
        val ordered: Boolean,
        /** 0-based nesting level (0 = top-level item). */
        val depth: Int,
        val number: Int?,
        /** null = not a task item; true = checked; false = unchecked. */
        val taskChecked: Boolean?,
        val spans: List<MdInline>
    ) : MdBlock
    data object HorizontalRule : MdBlock
}

/** Inline span kinds. Nested styles are represented by nesting [MdInline] values. */
sealed interface MdInline {
    data class Text(val text: String) : MdInline
    data class Bold(val children: List<MdInline>) : MdInline
    data class Italic(val children: List<MdInline>) : MdInline
    data class BoldItalic(val children: List<MdInline>) : MdInline
    data class Strikethrough(val children: List<MdInline>) : MdInline
    data class Highlight(val children: List<MdInline>) : MdInline
    data class Code(val text: String) : MdInline
    data class Link(val text: List<MdInline>, val href: String) : MdInline
    data class Image(val alt: String, val src: String) : MdInline
}

object MarkdownParser {

    // ── Block phase ─────────────────────────────────────────────────────────

    private val atxHeader = Regex("^#{1,6}(?:\\s|$)")
    private val setextH1 = Regex("^\\s*=+\\s*$")
    private val setextH2 = Regex("^\\s*-{2,}\\s*$")
    private val hr = Regex("^\\s{0,3}([*\\-_])(?:\\s*\\1){2,}\\s*$")
    private val fencedCodeStart = Regex("^\\s{0,3}(~~~+|```+)[ \\t]*([\\w/+#-]*)")
    private val blockQuote = Regex("^(\\s{0,3}>\\s?)+")
    private val listItem = Regex("^(\\s*)(?:(\\d{1,9})[.)]|([*\\-+]))\\s+")
    private val taskBox = Regex("^\\[([ xX])]\\s+")
    private val linkDefinition = Regex("^\\s*\\[[^]]+]:.*$")

    fun parse(markdown: String): List<MdBlock> {
        val lines = markdown.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        val blocks = mutableListOf<MdBlock>()

        var i = 0
        // Paragraph accumulator: consecutive non-blank lines merge into one paragraph.
        val paraLines = StringBuilder()
        var paraHasContent = false

        fun flushParagraph() {
            if (paraHasContent) {
                blocks += MdBlock.Paragraph(parseInline(paraLines.toString()))
                paraLines.clear()
                paraHasContent = false
            }
        }

        fun addParaLine(line: String) {
            if (paraHasContent) paraLines.append('\n')
            paraLines.append(line.trim())
            paraHasContent = true
        }

        while (i < lines.size) {
            val line = lines[i]

            // ── Fenced code block: consumes until matching close fence ──────
            val fence = fencedCodeStart.find(line)
            if (fence != null) {
                flushParagraph()
                val fenceToken = fence.groupValues[1]
                val fenceChar = fenceToken[0]
                val fenceLen = fenceToken.length
                val lang = fence.groupValues[2].ifBlank { null }
                val closeFence = Regex("^\\s{0,3}${Regex.escape(fenceChar.toString())}{$fenceLen,}\\s*$")
                val code = StringBuilder()
                i++
                while (i < lines.size && !closeFence.matches(lines[i])) {
                    if (code.isNotEmpty()) code.append('\n')
                    code.append(lines[i])
                    i++
                }
                if (i < lines.size) i++ // skip the closing fence line
                blocks += MdBlock.CodeBlock(code.toString(), lang)
                continue
            }

            // ── Blank line: paragraph separator ────────────────────────────
            if (line.isBlank()) {
                flushParagraph()
                i++
                continue
            }

            // ── Setext headers: "Title\n===" / "Title\n---" ────────────────
            // Checked BEFORE the horizontal rule: per CommonMark, a `---` or
            // `===` line directly under an open paragraph is a setext heading
            // underline, not a thematic break.
            if ((setextH1.matches(line) || setextH2.matches(line)) && paraHasContent) {
                val level = if (setextH1.matches(line)) 1 else 2
                blocks += MdBlock.Header(level, parseInline(paraLines.toString()))
                paraLines.clear()
                paraHasContent = false
                i++
                continue
            }

            // ── Horizontal rule ────────────────────────────────────────────
            if (hr.matches(line)) {
                flushParagraph()
                blocks += MdBlock.HorizontalRule
                i++
                continue
            }

            // ── ATX headers: "# text" … "###### text" ──────────────────────
            val atx = atxHeader.find(line)
            if (atx != null) {
                flushParagraph()
                val level = atx.value.count { it == '#' }
                // Optional closing hashes: " ## text ## "
                val text = line.substring(atx.value.length)
                    .replace(Regex("\\s+#+\\s*$"), "")
                    .trim()
                blocks += MdBlock.Header(level, parseInline(text))
                i++
                continue
            }

            // ── Link reference definitions are metadata, not content ───────
            if (linkDefinition.matches(line)) {
                flushParagraph()
                i++
                continue
            }

            // ── Block quote (supports nesting: "> > text") ─────────────────
            val quote = blockQuote.find(line)
            if (quote != null) {
                flushParagraph()
                val depth = quote.value.count { it == '>' }
                val text = line.substring(quote.value.length).trim()
                blocks += MdBlock.BlockQuote(depth, parseInline(text))
                i++
                continue
            }

            // ── List item (unordered / ordered / task, with nesting) ───────
            val item = listItem.find(line)
            if (item != null) {
                flushParagraph()
                val indent = expandTabs(item.groupValues[1]).length
                val ordered = item.groupValues[2].isNotEmpty()
                val number = item.groupValues[2].toIntOrNull()
                // 4 spaces per nesting level (CommonMark tab width), round up
                // so 2-space indented sub-lists still nest.
                val depth = (indent + 3) / 4
                var rest = line.substring(item.value.length)
                var taskChecked: Boolean? = null
                val task = taskBox.find(rest)
                if (task != null) {
                    taskChecked = task.groupValues[1] != " "
                    rest = rest.substring(task.value.length)
                }
                blocks += MdBlock.ListItem(
                    ordered = ordered,
                    depth = depth,
                    number = number,
                    taskChecked = taskChecked,
                    spans = parseInline(rest.trim())
                )
                i++
                continue
            }

            // ── Everything else: paragraph text (may merge with next lines) ─
            addParaLine(line)
            i++
        }
        flushParagraph()

        return blocks
    }

    private fun expandTabs(s: String) = s.replace("\t", "    ")

    // ── Inline phase ────────────────────────────────────────────────────────

    /** Chars CommonMark allows to be backslash-escaped (rendered literally). */
    private val escapable = "\\`*{}[]()#+-.!_>~|=:".toSet()

    /**
     * Recursive-descent inline parser. Scans [text] once, producing spans.
     * Longest delimiters are tried first (```***``` before `**` before `*`).
     * A delimiter that opens but never closes is emitted as literal text,
     * so malformed input (e.g. a half-typed `**bold`) degrades gracefully.
     */
    fun parseInline(text: String): List<MdInline> {
        val spans = mutableListOf<MdInline>()
        val plain = StringBuilder()

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                spans += MdInline.Text(plain.toString())
                plain.clear()
            }
        }

        fun StringBuilder.appendSpan(span: MdInline) {
            flushPlain()
            spans += span
        }

        var i = 0
        while (i < text.length) {
            val ch = text[i]

            // ── Backslash escape: next char is literal ─────────────────────
            if (ch == '\\' && i + 1 < text.length && text[i + 1] in escapable) {
                plain.append(text[i + 1])
                i += 2
                continue
            }

            // ── Inline code (supports multi-backtick delimiters: ``a`b``) ──
            if (ch == '`') {
                var tickCount = 0
                while (i + tickCount < text.length && text[i + tickCount] == '`') tickCount++
                val delimiter = "`".repeat(tickCount)
                val close = text.indexOf(delimiter, i + tickCount)
                if (close >= 0) {
                    val code = text.substring(i + tickCount, close)
                        .replace("\n", " ")
                        .let { if (it.length >= 2 && it.startsWith(" ") && it.endsWith(" ") && it.any { c -> c != ' ' }) it.drop(1).dropLast(1) else it }
                    plain.appendSpan(MdInline.Code(code))
                    i = close + tickCount
                    continue
                }
                // No closing backticks: literal
                plain.append(delimiter)
                i += tickCount
                continue
            }

            // ── Image: ![alt](src) — checked before link because of the '!' ─
            if (ch == '!' && i + 1 < text.length && text[i + 1] == '[') {
                val altClose = findClosingBracket(text, i + 1)
                if (altClose != null && altClose + 1 < text.length && text[altClose + 1] == '(') {
                    val parenClose = findClosingParen(text, altClose + 1)
                    if (parenClose != null) {
                        plain.appendSpan(
                            MdInline.Image(
                                alt = text.substring(i + 2, altClose),
                                src = text.substring(altClose + 2, parenClose).substringBefore(" ").trim()
                            )
                        )
                        i = parenClose + 1
                        continue
                    }
                }
            }

            // ── Link: [text](href) ─────────────────────────────────────────
            if (ch == '[') {
                val bracketClose = findClosingBracket(text, i)
                if (bracketClose != null && bracketClose + 1 < text.length && text[bracketClose + 1] == '(') {
                    val parenClose = findClosingParen(text, bracketClose + 1)
                    if (parenClose != null) {
                        val href = text.substring(bracketClose + 2, parenClose)
                            .substringBefore(" ") // drop optional "title"
                            .trim()
                        plain.appendSpan(
                            MdInline.Link(
                                text = parseInline(text.substring(i + 1, bracketClose)),
                                href = href
                            )
                        )
                        i = parenClose + 1
                        continue
                    }
                }
            }

            // ── Autolink: <https://…> / <mail@…> ──────────────────────────
            if (ch == '<') {
                val gt = text.indexOf('>', i + 1)
                if (gt > i + 1) {
                    val inside = text.substring(i + 1, gt)
                    if (inside.matches(Regex("^(https?|ftps?)://\\S+$")) ||
                        inside.matches(Regex("^\\S+@\\S+$"))
                    ) {
                        plain.appendSpan(MdInline.Link(listOf(MdInline.Text(inside)), inside))
                        i = gt + 1
                        continue
                    }
                }
            }

            // ── Bold / italic / bold-italic: *** ___ ** __ * _ ─────────────
            if (ch == '*' || ch == '_') {
                var run = 0
                while (i + run < text.length && text[i + run] == ch) run++
                when {
                    run >= 3 -> {
                        val close = findClosing(text, "***", i + 3) ?: findClosing(text, "___", i + 3)
                        if (close != null) {
                            plain.appendSpan(MdInline.BoldItalic(parseInline(text.substring(i + 3, close))))
                            i = close + 3
                            continue
                        }
                    }
                    run >= 2 -> {
                        val close = findClosing(text, ch.toString().repeat(2), i + 2)
                        if (close != null && close > i + 2) {
                            plain.appendSpan(MdInline.Bold(parseInline(text.substring(i + 2, close))))
                            i = close + 2
                            continue
                        }
                    }
                    else -> {
                        val close = findClosing(text, ch.toString(), i + 1)
                        if (close != null && close > i + 1) {
                            plain.appendSpan(MdInline.Italic(parseInline(text.substring(i + 1, close))))
                            i = close + 1
                            continue
                        }
                    }
                }
                // Unmatched: emit the run literally
                plain.append(ch.toString().repeat(run))
                i += run
                continue
            }

            // ── Strikethrough: ~~text~~ ────────────────────────────────────
            if (ch == '~' && i + 1 < text.length && text[i + 1] == '~') {
                val close = findClosing(text, "~~", i + 2)
                if (close != null && close > i + 2) {
                    plain.appendSpan(MdInline.Strikethrough(parseInline(text.substring(i + 2, close))))
                    i = close + 2
                    continue
                }
                plain.append("~~")
                i += 2
                continue
            }

            // ── Highlight: ==text== ────────────────────────────────────────
            if (ch == '=' && i + 1 < text.length && text[i + 1] == '=') {
                val close = findClosing(text, "==", i + 2)
                if (close != null && close > i + 2) {
                    plain.appendSpan(MdInline.Highlight(parseInline(text.substring(i + 2, close))))
                    i = close + 2
                    continue
                }
                plain.append("==")
                i += 2
                continue
            }

            plain.append(ch)
            i++
        }
        flushPlain()

        return spans
    }

    /**
     * Finds a closing [delimiter] starting the search at [from], skipping
     * escaped occurrences. Returns the index of the delimiter, or null.
     */
    private fun findClosing(text: String, delimiter: String, from: Int): Int? {
        var idx = from
        while (idx <= text.length - delimiter.length) {
            val found = text.indexOf(delimiter, idx)
            if (found < 0) return null
            // An escaped delimiter doesn't count as a closer
            if (found > 0 && text[found - 1] == '\\') {
                idx = found + 1
                continue
            }
            return found
        }
        return null
    }

    /** Closing index of a `[`…`]` pair (with nesting + escape awareness). */
    private fun findClosingBracket(text: String, open: Int): Int? =
        findMatchingPair(text, open, '[', ']')

    /** Closing index of a `(`…`)` pair (with nesting + escape awareness). */
    private fun findClosingParen(text: String, open: Int): Int? =
        findMatchingPair(text, open, '(', ')')

    private fun findMatchingPair(text: String, open: Int, openCh: Char, closeCh: Char): Int? {
        var depth = 0
        var i = open
        while (i < text.length) {
            val c = text[i]
            when {
                c == '\\' -> i++ // skip escaped char
                c == openCh -> depth++
                c == closeCh -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return null
    }
}
