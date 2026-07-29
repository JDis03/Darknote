package com.darknote.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.darknote.core.markdown.MdBlock
import com.darknote.core.markdown.MdInline
import com.darknote.core.markdown.MarkdownParser

/**
 * Read-only rendered Markdown preview.
 *
 * Renders the block model from [MarkdownParser] as real styled Compose
 * content: headers at scaled sizes, true bold/italic/strikethrough,
 * bulleted/numbered/task lists with indentation, block quotes with a
 * side bar, monospace code blocks, and clickable links.
 *
 * This is a pure display component — it never touches the source string,
 * so toggling between preview and the raw editor is lossless by design.
 */
@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Header -> HeaderBlock(block)
                is MdBlock.Paragraph -> ParagraphBlock(block)
                is MdBlock.CodeBlock -> CodeBlockView(block)
                is MdBlock.BlockQuote -> BlockQuoteView(block)
                is MdBlock.ListItem -> ListItemView(block)
                MdBlock.HorizontalRule -> HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

// ── Blocks ──────────────────────────────────────────────────────────────────

@Composable
private fun HeaderBlock(block: MdBlock.Header) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        4 -> MaterialTheme.typography.titleLarge
        5 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    Text(
        text = block.spans.toAnnotatedString(),
        style = style.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ParagraphBlock(block: MdBlock.Paragraph) {
    val uriHandler = LocalUriHandler.current
    val annotated = block.spans.toAnnotatedString()
    val linkAnnotations = remember(annotated) { collectLinks(block.spans) }

    if (linkAnnotations.isEmpty()) {
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    } else {
        ClickableText(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            onClick = { offset ->
                linkAnnotations.firstOrNull { offset in it.start until it.end }
                    ?.let { runCatching { uriHandler.openUri(it.href) } }
            }
        )
    }
}

@Composable
private fun CodeBlockView(block: MdBlock.CodeBlock) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            block.language?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            // Reuse the existing syntax highlighter so fenced code keeps its
            // language colors in preview, falling back to plain monospace.
            Text(
                text = SyntaxHighlighter.highlight(block.code, block.language),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BlockQuoteView(block: MdBlock.BlockQuote) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // One side bar per nesting level — the visual cue for quote depth.
        repeat(block.depth) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .width(3.dp)
                    .height(20.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        MaterialTheme.shapes.extraSmall
                    )
            )
        }
        Text(
            text = block.spans.toAnnotatedString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ListItemView(item: MdBlock.ListItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (item.depth * 20).dp)
    ) {
        val marker = when {
            item.taskChecked != null -> if (item.taskChecked == true) "☑" else "☐"
            item.ordered -> "${item.number ?: 1}."
            else -> "•"
        }
        Text(
            text = marker,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = item.spans.toAnnotatedString(),
            style = MaterialTheme.typography.bodyMedium.let { base ->
                if (item.taskChecked == true) base.copy(textDecoration = TextDecoration.LineThrough) else base
            },
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Inline rendering ────────────────────────────────────────────────────────

private data class LinkRange(val start: Int, val end: Int, val href: String)

/**
 * Converts inline spans to an [AnnotatedString]. Link hrefs are also
 * recorded as string annotations ("URL") so [collectLinks] can map them.
 */
private fun List<MdInline>.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    appendSpans(this@toAnnotatedString, baseStyle = SpanStyle())
}

private fun AnnotatedString.Builder.appendSpans(spans: List<MdInline>, baseStyle: SpanStyle) {
    for (span in spans) {
        when (span) {
            is MdInline.Text -> withStyle(baseStyle) { append(span.text) }
            is MdInline.Bold -> appendSpans(
                span.children,
                baseStyle.merge(SpanStyle(fontWeight = FontWeight.Bold))
            )
            is MdInline.Italic -> appendSpans(
                span.children,
                baseStyle.merge(SpanStyle(fontStyle = FontStyle.Italic))
            )
            is MdInline.BoldItalic -> appendSpans(
                span.children,
                baseStyle.merge(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
            )
            is MdInline.Strikethrough -> appendSpans(
                span.children,
                baseStyle.merge(SpanStyle(textDecoration = TextDecoration.LineThrough))
            )
            is MdInline.Highlight -> appendSpans(
                span.children,
                baseStyle.merge(SpanStyle(background = androidx.compose.ui.graphics.Color(0x55FFEB3B)))
            )
            is MdInline.Code -> withStyle(
                baseStyle.merge(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = androidx.compose.ui.graphics.Color(0x22808080)
                    )
                )
            ) { append(span.text) }
            is MdInline.Link -> {
                val start = length
                appendSpans(
                    span.text,
                    baseStyle.merge(
                        SpanStyle(
                            color = androidx.compose.ui.graphics.Color(0xFF42A5F5),
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
                addStringAnnotation("URL", span.href, start, length)
            }
            is MdInline.Image -> withStyle(
                baseStyle.merge(SpanStyle(fontStyle = FontStyle.Italic))
            ) { append("[img: ${span.alt.ifBlank { span.src }}]") }
        }
    }
}

/** Extracts URL annotations (position + href) from parsed link spans. */
private fun collectLinks(spans: List<MdInline>): List<LinkRange> {
    // Position must match appendSpans output exactly — rebuild the string
    // and read back the annotations.
    val annotated = spans.toAnnotatedString()
    return annotated.getStringAnnotations("URL", 0, annotated.length)
        .map { LinkRange(it.start, it.end, it.item) }
}
