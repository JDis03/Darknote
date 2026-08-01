package com.darknote.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darknote.core.markdown.MarkdownParser
import com.darknote.core.markdown.MarkdownSerializer
import com.darknote.core.markdown.MdBlock
import com.darknote.core.markdown.MdInline

/**
 * Renderiza Markdown real usando el parser propio de DarkNote:
 * headings con tamaños, code blocks con fondo, blockquotes con borde,
 * listas con viñetas, imágenes como placeholders, etc.
 *
 * Cada bloque tiene su propio botón de copiar (Notion/Obsidian/GitHub
 * style) que copia SOLO el markdown de ese bloque — reconstruido vía
 * [MarkdownSerializer], nunca el texto ya renderizado.
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block -> BlockWithCopy(block) }
    }
}

/**
 * Wraps a single block with a copy button. Code blocks always show the
 * button (GitHub/VS Code convention); other blocks reveal it on tap and
 * also copy directly on long-press (with haptic feedback).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlockWithCopy(block: MdBlock) {
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var showCopy by remember { mutableStateOf(false) }
    val isCodeBlock = block is MdBlock.CodeBlock
    val copyVisible = isCodeBlock || showCopy

    fun copyBlock() {
        clipboard.setText(AnnotatedString(MarkdownSerializer.serialize(block)))
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (copyVisible) 40.dp else 0.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showCopy = !showCopy },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        copyBlock()
                    }
                )
        ) {
            BlockRenderer(block)
        }

        AnimatedVisibility(
            visible = copyVisible,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    copyBlock()
                    showCopy = false
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar bloque",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
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
        is MdBlock.Table -> TableRenderer(block)
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

/**
 * Renders a GFM-style table. Rows with fewer cells than [MdBlock.Table.headers]
 * are padded with empty cells — the parser reports rows exactly as written
 * (see MarkdownParserTest), padding is purely a display-layer concern here.
 */
@Composable
private fun TableRenderer(block: MdBlock.Table) {
    val colCount = block.headers.size.coerceAtLeast(1)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                block.headers.forEachIndexed { idx, cellSpans ->
                    val align = block.alignments.getOrElse(idx) { MdBlock.Table.ColumnAlignment.NONE }
                    TableCell(
                        spans = cellSpans,
                        isHeader = true,
                        alignment = align,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outline
            )

            block.rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { idx, cellSpans ->
                        val align = block.alignments.getOrElse(idx) { MdBlock.Table.ColumnAlignment.NONE }
                        TableCell(
                            spans = cellSpans,
                            isHeader = false,
                            alignment = align,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat((colCount - row.size).coerceAtLeast(0)) {
                        TableCell(
                            spans = emptyList(),
                            isHeader = false,
                            alignment = MdBlock.Table.ColumnAlignment.NONE,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun TableCell(
    spans: List<MdInline>,
    isHeader: Boolean,
    alignment: MdBlock.Table.ColumnAlignment,
    modifier: Modifier = Modifier
) {
    val textAlign = when (alignment) {
        MdBlock.Table.ColumnAlignment.LEFT -> TextAlign.Left
        MdBlock.Table.ColumnAlignment.CENTER -> TextAlign.Center
        MdBlock.Table.ColumnAlignment.RIGHT -> TextAlign.Right
        MdBlock.Table.ColumnAlignment.NONE -> TextAlign.Start
    }

    Box(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = when (alignment) {
            MdBlock.Table.ColumnAlignment.CENTER -> Alignment.Center
            MdBlock.Table.ColumnAlignment.RIGHT -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = buildInlineAnnotatedString(spans),
            style = if (isHeader) {
                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
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
