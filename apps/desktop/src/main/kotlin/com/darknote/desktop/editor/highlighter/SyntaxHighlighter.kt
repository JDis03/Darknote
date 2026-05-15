package com.darknote.desktop.editor.highlighter

import androidx.compose.ui.text.AnnotatedString
import com.darknote.desktop.editor.theme.EditorColorScheme

/**
 * Interface for syntax highlighting implementations.
 */
interface SyntaxHighlighter {
    /**
     * Highlights the given text according to the language syntax.
     * 
     * @param text The source code to highlight
     * @param colorScheme Color scheme to use for highlighting
     * @return AnnotatedString with applied syntax highlighting
     */
    fun highlight(text: String, colorScheme: EditorColorScheme): AnnotatedString
}

/**
 * Factory for creating language-specific syntax highlighters.
 */
object SyntaxHighlighterFactory {
    /**
     * Creates a syntax highlighter for the given language.
     * 
     * @param language Language identifier (e.g., "kotlin", "java", "python")
     * @return SyntaxHighlighter instance, or PlainTextHighlighter if language not supported
     */
    fun create(language: String?): SyntaxHighlighter {
        return when (language?.lowercase()) {
            "kotlin", "kt" -> KotlinHighlighter()
            "java" -> JavaHighlighter()
            "python", "py" -> PythonHighlighter()
            "javascript", "js" -> JavaScriptHighlighter()
            "typescript", "ts" -> TypeScriptHighlighter()
            "rust", "rs" -> RustHighlighter()
            "go" -> GoHighlighter()
            "c", "cpp", "c++" -> CppHighlighter()
            "json" -> JsonHighlighter()
            "xml", "html" -> XmlHighlighter()
            "sql" -> SqlHighlighter()
            "markdown", "md" -> MarkdownHighlighter()
            else -> PlainTextHighlighter()
        }
    }
}

/**
 * Plain text highlighter (no highlighting).
 */
class PlainTextHighlighter : SyntaxHighlighter {
    override fun highlight(text: String, colorScheme: EditorColorScheme): AnnotatedString {
        return AnnotatedString(text)
    }
}
