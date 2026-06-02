package com.darknote.desktop.editor.highlighter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.darknote.desktop.editor.theme.EditorColorScheme

/**
 * Syntax highlighter for Kotlin language.
 * 
 * Supports:
 * - Keywords (class, fun, val, var, if, when, etc.)
 * - Types (Int, String, List, etc.)
 * - String literals (single and multi-line)
 * - Comments (single and multi-line)
 * - Numbers
 * - Annotations (@Composable, @Serializable, etc.)
 */
open class KotlinHighlighter : SyntaxHighlighter {
    
    private val keywords = setOf(
        "class", "interface", "object", "fun", "val", "var", "if", "else", "when",
        "for", "while", "do", "return", "break", "continue", "try", "catch", "finally",
        "throw", "throws", "import", "package", "public", "private", "protected",
        "internal", "abstract", "final", "open", "override", "data", "sealed",
        "companion", "init", "constructor", "this", "super", "null", "true", "false",
        "is", "as", "in", "!in", "suspend", "inline", "noinline", "crossinline",
        "reified", "lateinit", "by", "where", "typealias", "expect", "actual",
        "enum", "annotation", "out", "tailrec", "infix", "operator", "const"
    )
    
    private val primitiveTypes = setOf(
        "Int", "Long", "Short", "Byte", "Float", "Double", "Boolean", "Char",
        "String", "Unit", "Any", "Nothing", "Array", "List", "Set", "Map",
        "MutableList", "MutableSet", "MutableMap", "Pair", "Triple"
    )
    
    override fun highlight(text: String, colorScheme: EditorColorScheme): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            
            while (i < text.length) {
                when {
                    // Single line comment
                    text.startsWith("//", i) -> {
                        val end = text.indexOf('\n', i).takeIf { it != -1 } ?: text.length
                        withStyle(SpanStyle(color = colorScheme.comment)) {
                            append(text.substring(i, end))
                        }
                        i = end
                    }
                    
                    // Multi-line comment
                    text.startsWith("/*", i) -> {
                        val end = text.indexOf("*/", i + 2)
                            .takeIf { it != -1 }
                            ?.let { it + 2 }
                            ?: text.length
                        withStyle(SpanStyle(color = colorScheme.comment)) {
                            append(text.substring(i, end))
                        }
                        i = end
                    }
                    
                    // Triple-quoted string (raw string)
                    text.startsWith("\"\"\"", i) -> {
                        val end = text.indexOf("\"\"\"", i + 3)
                            .takeIf { it != -1 }
                            ?.let { it + 3 }
                            ?: text.length
                        withStyle(SpanStyle(color = colorScheme.string)) {
                            append(text.substring(i, end))
                        }
                        i = end
                    }
                    
                    // String literal
                    text[i] == '"' -> {
                        val start = i
                        i++
                        while (i < text.length && text[i] != '"') {
                            if (text[i] == '\\' && i + 1 < text.length) i++
                            i++
                        }
                        if (i < text.length) i++ // Skip closing quote
                        
                        withStyle(SpanStyle(color = colorScheme.string)) {
                            append(text.substring(start, i))
                        }
                    }
                    
                    // Char literal
                    text[i] == '\'' -> {
                        val start = i
                        i++
                        if (i < text.length && text[i] == '\\') i++ // Escape sequence
                        if (i < text.length) i++ // Character
                        if (i < text.length && text[i] == '\'') i++ // Closing quote
                        
                        withStyle(SpanStyle(color = colorScheme.string)) {
                            append(text.substring(start, i))
                        }
                    }
                    
                    // Number (decimal, hex, binary, float)
                    text[i].isDigit() || (text[i] == '.' && i + 1 < text.length && text[i + 1].isDigit()) -> {
                        val start = i
                        
                        // Hex/Binary prefix
                        if (text[i] == '0' && i + 1 < text.length) {
                            when (text[i + 1].lowercaseChar()) {
                                'x' -> i += 2 // 0x
                                'b' -> i += 2 // 0b
                            }
                        }
                        
                        // Main number part
                        while (i < text.length && (
                            text[i].isLetterOrDigit() || 
                            text[i] in "._"
                        )) {
                            i++
                        }
                        
                        // Type suffix (L, f, F, etc.)
                        if (i < text.length && text[i] in "LlFfDd") {
                            i++
                        }
                        
                        withStyle(SpanStyle(color = colorScheme.number)) {
                            append(text.substring(start, i))
                        }
                    }
                    
                    // Annotation
                    text[i] == '@' -> {
                        val start = i
                        i++
                        while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) {
                            i++
                        }
                        withStyle(SpanStyle(color = colorScheme.annotation)) {
                            append(text.substring(start, i))
                        }
                    }
                    
                    // Identifier (keyword, type, or variable)
                    text[i].isLetter() || text[i] == '_' -> {
                        val start = i
                        while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) {
                            i++
                        }
                        val word = text.substring(start, i)
                        
                        when {
                            word in keywords -> {
                                withStyle(SpanStyle(
                                    color = colorScheme.keyword,
                                    fontWeight = FontWeight.Bold
                                )) {
                                    append(word)
                                }
                            }
                            word in primitiveTypes -> {
                                withStyle(SpanStyle(color = colorScheme.type)) {
                                    append(word)
                                }
                            }
                            word.firstOrNull()?.isUpperCase() == true -> {
                                // Likely a class/type name
                                withStyle(SpanStyle(color = colorScheme.type)) {
                                    append(word)
                                }
                            }
                            word == word.uppercase() && word.length > 1 -> {
                                // All caps - likely a constant
                                withStyle(SpanStyle(color = colorScheme.constant)) {
                                    append(word)
                                }
                            }
                            else -> {
                                append(word)
                            }
                        }
                    }
                    
                    // Operator
                    text[i] in "+-*/%=<>!&|^~" -> {
                        withStyle(SpanStyle(color = colorScheme.operator)) {
                            append(text[i])
                        }
                        i++
                    }
                    
                    // Default: append as-is
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
 * Java highlighter - similar to Kotlin with slight differences.
 */
class JavaHighlighter : KotlinHighlighter()

/**
 * JavaScript/TypeScript highlighter - similar to Kotlin.
 */
class JavaScriptHighlighter : KotlinHighlighter()
class TypeScriptHighlighter : KotlinHighlighter()

/**
 * Rust highlighter - similar to Kotlin.
 */
class RustHighlighter : KotlinHighlighter()

/**
 * Go highlighter - similar to Kotlin.
 */
class GoHighlighter : KotlinHighlighter()

/**
 * C/C++ highlighter - similar to Kotlin.
 */
class CppHighlighter : KotlinHighlighter()
