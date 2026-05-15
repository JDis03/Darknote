package com.darknote.desktop.editor.highlighter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.darknote.desktop.editor.theme.EditorColorScheme

/**
 * Python syntax highlighter.
 */
class PythonHighlighter : SyntaxHighlighter {
    private val keywords = setOf(
        "def", "class", "if", "elif", "else", "for", "while", "try", "except",
        "finally", "import", "from", "as", "return", "break", "continue", "pass",
        "yield", "lambda", "with", "raise", "assert", "del", "global", "nonlocal",
        "True", "False", "None", "and", "or", "not", "in", "is", "async", "await"
    )
    
    override fun highlight(text: String, colorScheme: EditorColorScheme): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    text[i] == '#' -> {
                        val end = text.indexOf('\n', i).takeIf { it != -1 } ?: text.length
                        withStyle(SpanStyle(color = colorScheme.comment)) {
                            append(text.substring(i, end))
                        }
                        i = end
                    }
                    text[i] in "\"'" -> {
                        val quote = text[i]
                        val start = i
                        i++
                        while (i < text.length && text[i] != quote) {
                            if (text[i] == '\\') i++
                            i++
                        }
                        if (i < text.length) i++
                        withStyle(SpanStyle(color = colorScheme.string)) {
                            append(text.substring(start, i))
                        }
                    }
                    text[i].isLetter() || text[i] == '_' -> {
                        val start = i
                        while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) {
                            i++
                        }
                        val word = text.substring(start, i)
                        if (word in keywords) {
                            withStyle(SpanStyle(color = colorScheme.keyword, fontWeight = FontWeight.Bold)) {
                                append(word)
                            }
                        } else {
                            append(word)
                        }
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }
}

/**
 * JSON syntax highlighter.
 */
class JsonHighlighter : SyntaxHighlighter {
    override fun highlight(text: String, colorScheme: EditorColorScheme): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    text[i] == '"' -> {
                        val start = i
                        i++
                        while (i < text.length && text[i] != '"') {
                            if (text[i] == '\\') i++
                            i++
                        }
                        if (i < text.length) i++
                        
                        // Check if it's a key (followed by :)
                        val afterQuote = text.drop(i).trimStart()
                        val isKey = afterQuote.startsWith(':')
                        
                        withStyle(SpanStyle(color = if (isKey) colorScheme.constant else colorScheme.string)) {
                            append(text.substring(start, i))
                        }
                    }
                    text[i].isDigit() || (text[i] == '-' && i + 1 < text.length && text[i + 1].isDigit()) -> {
                        val start = i
                        if (text[i] == '-') i++
                        while (i < text.length && (text[i].isDigit() || text[i] in ".eE+-")) {
                            i++
                        }
                        withStyle(SpanStyle(color = colorScheme.number)) {
                            append(text.substring(start, i))
                        }
                    }
                    text.startsWith("true", i) || text.startsWith("false", i) || text.startsWith("null", i) -> {
                        val word = when {
                            text.startsWith("true", i) -> "true"
                            text.startsWith("false", i) -> "false"
                            else -> "null"
                        }
                        withStyle(SpanStyle(color = colorScheme.keyword, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                        i += word.length
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }
}

/**
 * XML/HTML syntax highlighter.
 */
class XmlHighlighter : SyntaxHighlighter {
    override fun highlight(text: String, colorScheme: EditorColorScheme): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    text.startsWith("<!--", i) -> {
                        val end = text.indexOf("-->", i + 4)
                            .takeIf { it != -1 }
                            ?.let { it + 3 }
                            ?: text.length
                        withStyle(SpanStyle(color = colorScheme.comment)) {
                            append(text.substring(i, end))
                        }
                        i = end
                    }
                    text[i] == '<' -> {
                        val end = text.indexOf('>', i).takeIf { it != -1 }?.let { it + 1 } ?: text.length
                        withStyle(SpanStyle(color = colorScheme.keyword)) {
                            append(text.substring(i, end))
                        }
                        i = end
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }
}

/**
 * SQL syntax highlighter.
 */
class SqlHighlighter : SyntaxHighlighter {
    private val keywords = setOf(
        "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP",
        "ALTER", "TABLE", "INDEX", "JOIN", "INNER", "LEFT", "RIGHT", "OUTER",
        "ON", "AND", "OR", "NOT", "NULL", "IS", "AS", "ORDER", "BY", "GROUP",
        "HAVING", "LIMIT", "OFFSET", "DISTINCT", "COUNT", "SUM", "AVG", "MAX", "MIN",
        "UNION", "EXISTS", "LIKE", "IN", "BETWEEN", "CASE", "WHEN", "THEN", "ELSE", "END"
    )
    
    override fun highlight(text: String, colorScheme: EditorColorScheme): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    text.startsWith("--", i) -> {
                        val end = text.indexOf('\n', i).takeIf { it != -1 } ?: text.length
                        withStyle(SpanStyle(color = colorScheme.comment)) {
                            append(text.substring(i, end))
                        }
                        i = end
                    }
                    text[i] == '\'' -> {
                        val start = i
                        i++
                        while (i < text.length && text[i] != '\'') {
                            if (text[i] == '\\') i++
                            i++
                        }
                        if (i < text.length) i++
                        withStyle(SpanStyle(color = colorScheme.string)) {
                            append(text.substring(start, i))
                        }
                    }
                    text[i].isLetter() || text[i] == '_' -> {
                        val start = i
                        while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) {
                            i++
                        }
                        val word = text.substring(start, i)
                        if (word.uppercase() in keywords) {
                            withStyle(SpanStyle(color = colorScheme.keyword, fontWeight = FontWeight.Bold)) {
                                append(word)
                            }
                        } else {
                            append(word)
                        }
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }
}

/**
 * Markdown syntax highlighter.
 */
class MarkdownHighlighter : SyntaxHighlighter {
    override fun highlight(text: String, colorScheme: EditorColorScheme): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val lines = text.lines()
            
            for ((lineIndex, line) in lines.withIndex()) {
                when {
                    // Headers
                    line.trimStart().startsWith("#") -> {
                        withStyle(SpanStyle(color = colorScheme.keyword, fontWeight = FontWeight.Bold)) {
                            append(line)
                        }
                    }
                    // Code blocks
                    line.trimStart().startsWith("```") -> {
                        withStyle(SpanStyle(color = colorScheme.string)) {
                            append(line)
                        }
                    }
                    // Lists
                    line.trimStart().matches(Regex("^[*\\-+]\\s.*")) -> {
                        withStyle(SpanStyle(color = colorScheme.keyword)) {
                            append(line)
                        }
                    }
                    else -> append(line)
                }
                
                if (lineIndex < lines.size - 1) {
                    append('\n')
                }
            }
        }
    }
}
