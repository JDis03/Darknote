package com.darknote.android.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

data class TokenRule(
    val pattern: Regex,
    val style: SpanStyle
)

data class LanguageGrammar(
    val keywords: Set<String>,
    val singleLineComment: String? = null,
    val stringDelimiters: List<String> = listOf("\"", "'"),
    val extraRules: List<TokenRule> = emptyList()
) {
    fun buildRules(
        keywordStyle: SpanStyle,
        commentStyle: SpanStyle,
        stringStyle: SpanStyle,
        numberStyle: SpanStyle
    ): List<TokenRule> {
        val rules = mutableListOf<TokenRule>()

        singleLineComment?.let { prefix ->
            rules.add(TokenRule(Regex("$prefix.*"), commentStyle))
        }

        stringDelimiters.forEach { delim ->
            val escaped = Regex.escape(delim)
            rules.add(TokenRule(Regex("$escaped(?:[^$escaped\\\\]|\\\\.)*$escaped"), stringStyle))
        }

        rules.add(TokenRule(Regex("\\b(?:${keywords.joinToString("|") { Regex.escape(it) }})\\b"), keywordStyle))
        rules.add(TokenRule(Regex("\\b\\d+(\\.\\d+)?\\b"), numberStyle))

        rules.addAll(extraRules)

        return rules
    }
}

val languageGrammars = mapOf(
    "kotlin" to LanguageGrammar(
        keywords = setOf(
            "val", "var", "fun", "class", "object", "interface", "data", "sealed",
            "when", "if", "else", "for", "while", "do", "return", "break", "continue",
            "true", "false", "null", "import", "package", "suspend", "override",
            "private", "public", "internal", "protected", "abstract", "open", "final",
            "companion", "init", "constructor", "this", "super", "is", "as", "in", "out",
            "by", "throw", "try", "catch", "finally", "typealias", "enum"
        ),
        singleLineComment = "//",
        stringDelimiters = listOf("\"", "\"\"\""),
        extraRules = listOf(
            TokenRule(Regex("@\\w+"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFFFA726)))
        )
    ),
    "python" to LanguageGrammar(
        keywords = setOf(
            "def", "class", "import", "from", "as", "if", "elif", "else",
            "for", "while", "break", "continue", "return", "yield", "lambda",
            "try", "except", "finally", "raise", "with", "pass", "True", "False",
            "None", "and", "or", "not", "in", "is", "self", "async", "await",
            "global", "nonlocal", "assert", "del"
        ),
        singleLineComment = "#",
        extraRules = listOf(
            TokenRule(Regex("@\\w+"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFFFA726)))
        )
    ),
    "bash" to LanguageGrammar(
        keywords = setOf(
            "if", "then", "else", "elif", "fi", "for", "while", "do", "done",
            "case", "esac", "in", "function", "return", "exit", "export", "local",
            "source", "alias", "echo", "read", "shift", "trap", "wait", "eval",
            "exec", "set", "unset", "declare", "typeset"
        ),
        singleLineComment = "#",
        extraRules = listOf(
            TokenRule(Regex("\\$[{]?\\w+[}]?"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF66BB6A))),
            TokenRule(Regex("^\\s*\\w+\\s*\\(\\)"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5)))
        )
    ),
    "javascript" to LanguageGrammar(
        keywords = setOf(
            "const", "let", "var", "function", "class", "extends", "import", "export",
            "default", "if", "else", "for", "while", "do", "switch", "case", "break",
            "continue", "return", "yield", "await", "async", "throw", "try", "catch",
            "finally", "new", "this", "super", "typeof", "instanceof", "true", "false",
            "null", "undefined", "of", "in", "from"
        ),
        singleLineComment = "//",
        stringDelimiters = listOf("\"", "'", "`")
    ),
    "typescript" to LanguageGrammar(
        keywords = setOf(
            "const", "let", "var", "function", "class", "extends", "implements",
            "interface", "type", "enum", "import", "export", "default", "if", "else",
            "for", "while", "do", "switch", "case", "break", "continue", "return",
            "yield", "await", "async", "throw", "try", "catch", "finally", "new",
            "this", "super", "typeof", "instanceof", "true", "false", "null",
            "undefined", "of", "in", "from", "as", "readonly", "private", "public",
            "protected", "abstract", "static", "keyof", "never", "unknown", "any",
            "string", "number", "boolean", "void"
        ),
        singleLineComment = "//",
        stringDelimiters = listOf("\"", "'", "`"),
        extraRules = listOf(
            TokenRule(Regex("@\\w+"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFFFA726)))
        )
    ),
    "sql" to LanguageGrammar(
        keywords = setOf(
            "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE",
            "ALTER", "DROP", "TABLE", "INDEX", "VIEW", "JOIN", "LEFT", "RIGHT",
            "INNER", "OUTER", "ON", "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN",
            "LIKE", "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET", "AS",
            "DISTINCT", "COUNT", "SUM", "AVG", "MAX", "MIN", "NULL", "PRIMARY",
            "KEY", "FOREIGN", "REFERENCES", "CASCADE", "SET", "VALUES", "INTO",
            "DEFAULT", "UNIQUE", "CHECK", "CONSTRAINT", "INTEGER", "VARCHAR",
            "TEXT", "BOOLEAN", "TIMESTAMP", "BEGIN", "COMMIT", "ROLLBACK"
        ),
        singleLineComment = "--"
    ),
    "config" to LanguageGrammar(
        keywords = setOf("true", "false", "yes", "no", "on", "off"),
        singleLineComment = "#",
        extraRules = listOf(
            TokenRule(Regex("^\\s*\\[\\w+\\]"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5))),
            TokenRule(Regex("^\\s*\\w+\\s*[=:]"), SpanStyle(color = Color(0xFFAB47BC)))
        )
    ),
    "yaml" to LanguageGrammar(
        keywords = setOf("true", "false", "yes", "no", "on", "off", "null"),
        singleLineComment = "#",
        extraRules = listOf(
            TokenRule(Regex("^\\s*[\\w-]+\\s*:"), SpanStyle(color = Color(0xFFAB47BC)))
        )
    ),
    "json" to LanguageGrammar(
        keywords = setOf("true", "false", "null"),
        extraRules = listOf(
            TokenRule(Regex("\"[^\"]*\"\\s*:"), SpanStyle(color = Color(0xFFAB47BC)))
        )
    ),
    "cpp" to LanguageGrammar(
        keywords = setOf(
            "auto", "break", "case", "class", "const", "continue", "default",
            "delete", "do", "else", "enum", "explicit", "extern", "false", "for",
            "friend", "goto", "if", "inline", "int", "long", "namespace", "new",
            "noexcept", "nullptr", "operator", "override", "private", "protected",
            "public", "return", "short", "signed", "sizeof", "static", "struct",
            "switch", "template", "this", "throw", "true", "try", "typedef",
            "typename", "union", "unsigned", "using", "virtual", "void", "volatile",
            "while", "include", "define"
        ),
        singleLineComment = "//",
        extraRules = listOf(
            TokenRule(Regex("#\\s*include\\s*[<\"]\\S+[>\"]"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFEF5350)))
        )
    )
)

object SyntaxHighlighter {

    private val keywordStyle = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5))
    private val commentStyle = SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF6D6D6D))
    private val stringStyle = SpanStyle(color = Color(0xFF66BB6A))
    private val numberStyle = SpanStyle(color = Color(0xFFFFA726))

    fun highlight(code: String, language: String?): androidx.compose.ui.text.AnnotatedString {
        if (language == null || code.isBlank()) {
            return buildAnnotatedString { append(code) }
        }

        val grammar = languageGrammars[language.lowercase()]
            ?: languageGrammars[normalizeLanguage(language)]
            ?: return buildAnnotatedString { append(code) }

        val rules = grammar.buildRules(keywordStyle, commentStyle, stringStyle, numberStyle)

        return buildAnnotatedString {
            append(code)

            for (rule in rules) {
                for (match in rule.pattern.findAll(code)) {
                    val range = match.range
                    if (range.first >= 0 && range.last + 1 <= code.length) {
                        addStyle(rule.style, range.first, range.last + 1)
                    }
                }
            }
        }
    }

    private fun normalizeLanguage(lang: String): String = when (lang.lowercase()) {
        "py" -> "python"
        "kt", "kts" -> "kotlin"
        "js", "mjs" -> "javascript"
        "ts", "mts" -> "typescript"
        "sh", "shell", "zsh" -> "bash"
        "yml" -> "yaml"
        "c", "cpp", "cxx", "h", "hpp" -> "cpp"
        else -> lang.lowercase()
    }
}
