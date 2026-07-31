package com.darknote.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darknote.core.markdown.MdBlock
import com.darknote.core.markdown.MdInline
import com.darknote.core.markdown.MarkdownParser

/**
 * Renderiza Markdown real usando el parser propio de DarkNote:
 * headings con tamaños, code blocks con fondo, blockquotes con borde,
 * listas con viñetas, imágenes como placeholders, etc.
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
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        blocks.forEach { block -> BlockRenderer(block) }
    }
}

@Composable
private fun BlockRenderer(block: MdBlock) {
    when (block) {
        is MdBlock.Header -> HeaderBlock(block)
        is MdBlock.Paragraph -> ParagraphBlock(block)
        is MdBlock.CodeBlock -> CodeBlockRenderer(block)
        is MdBlock.BlockQuote -> BlockQuoteRenderer(block)
        is MdBlock.ListItem -> ListItemRenderer(block)
        is MdBlock.HorizontalRule -> HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

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
        text = buildInlineAnnotatedString(block.spans),
        style = style,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ParagraphBlock(block: MdBlock.Paragraph) {
    Text(
        text = buildInlineAnnotatedString(block.spans),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 24.sp
    )
}

@Composable
private fun CodeBlockRenderer(block: MdBlock.CodeBlock) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            block.language?.let { lang ->
                Text(
                    text = lang,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = block.code,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BlockQuoteRenderer(block: MdBlock.BlockQuote) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (block.depth * 12).dp)
            .drawBehind {
                drawLine(
                    color = primaryColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 4f
                )
            }
            .padding(start = 12.dp)
    ) {
        Text(
            text = buildInlineAnnotatedString(block.spans),
            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ListItemRenderer(block: MdBlock.ListItem) {
    val bullet = when {
        block.taskChecked == true -> "☑ "
        block.taskChecked == false -> "☐ "
        block.ordered -> "${block.number}. "
        else -> "• "
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (block.depth * 16).dp)
    ) {
        Text(
            text = bullet,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = buildInlineAnnotatedString(block.spans),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun buildInlineAnnotatedString(spans: List<MdInline>): AnnotatedString {
    return buildAnnotatedString {
        spans.forEach { span -> appendInline(span) }
    }
}

private fun AnnotatedString.Builder.appendInline(span: MdInline) {
    when (span) {
        is MdInline.Text -> append(span.text)
        is MdInline.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            span.children.forEach { appendInline(it) }
        }
        is MdInline.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            span.children.forEach { appendInline(it) }
        }
        is MdInline.BoldItalic -> withStyle(
            SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        ) {
            span.children.forEach { appendInline(it) }
        }
        is MdInline.Strikethrough -> withStyle(
            SpanStyle(textDecoration = TextDecoration.LineThrough)
        ) {
            span.children.forEach { appendInline(it) }
        }
        is MdInline.Highlight -> withStyle(
            SpanStyle(background = Color(0xFFFFF59D))
        ) {
            span.children.forEach { appendInline(it) }
        }
        is MdInline.Code -> withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color(0xFF2D2D2D),
                color = Color(0xFFE0E0E0)
            )
        ) {
            append(span.text)
        }
        is MdInline.Link -> withStyle(
            SpanStyle(
                color = Color(0xFF2196F3),
                textDecoration = TextDecoration.Underline
            )
        ) {
            span.text.forEach { appendInline(it) }
        }
        is MdInline.Image -> append("[img: ${span.alt}]")
    }
}
