package com.darknote.android.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.util.concurrent.ConcurrentHashMap

data class TokenRule(
    val pattern: Regex,
    val style: SpanStyle
)

data class LanguageGrammar(
    val keywords: Set<String>,
    val singleLineComment: String? = null,
    val stringDelimiters: List<String> = listOf("\"", "'"),
    val blockCommentPairs: List<Pair<String, String>> = emptyList(),
    /** Multi-line string delimiters, e.g. Kotlin/Python triple quotes. */
    val rawStringDelimiters: List<String> = emptyList(),
    /** SQL and friends: keywords match regardless of letter case. */
    val keywordIgnoreCase: Boolean = false,
    val extraRules: List<TokenRule> = emptyList()
) {
    /**
     * Builds the rules in priority order: in an AnnotatedString, styles added
     * LATER win on overlapping ranges, so the list runs from lowest to highest
     * priority:
     *
     *   keywords -> numbers -> block comments -> line comments -> strings -> extraRules
     *
     * Consequences of this order:
     * - Keywords/numbers inside comments or strings are NOT re-colored (the
     *   comment/string style overrides them).
     * - Strings rank above comments so URLs such as "https://…" inside a string
     *   are not mistaken for a "//" line-comment start.
     * - extraRules rank highest so language-specific constructs (JSON keys,
     *   markdown code fences, bash $vars, cpp #include) always win.
     */
    fun buildRules(
        keywordStyle: SpanStyle,
        commentStyle: SpanStyle,
        stringStyle: SpanStyle,
        numberStyle: SpanStyle
    ): List<TokenRule> {
        val rules = mutableListOf<TokenRule>()

        // 1. Keywords (lowest priority). Skipped entirely for grammars without
        //    keywords (markdown) — an empty alternation would produce useless
        //    zero-width matches on every word boundary.
        if (keywords.isNotEmpty()) {
            val options = if (keywordIgnoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
            rules.add(
                TokenRule(
                    Regex("\\b(?:${keywords.joinToString("|") { Regex.escape(it) }})\\b", options),
                    keywordStyle
                )
            )
        }

        // 2. Numbers: hex (0xFF), binary (0b101), digit separators (1_000),
        //    decimals, exponents (1e10) and type suffixes (1f, 2L).
        rules.add(
            TokenRule(
                Regex("\\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|\\d[\\d_]*(?:\\.\\d+)?(?:[eE][+-]?\\d+)?[fFLl]?)\\b"),
                numberStyle
            )
        )

        // 3. Block comments /* ... */
        blockCommentPairs.forEach { (start, end) ->
            rules.add(
                TokenRule(
                    Regex("${Regex.escape(start)}[\\s\\S]*?${Regex.escape(end)}"),
                    commentStyle
                )
            )
        }

        // 4. Line comments
        singleLineComment?.let { prefix ->
            rules.add(TokenRule(Regex("${Regex.escape(prefix)}[^\\n]*"), commentStyle))
        }

        // 5. Strings. Backticks (JS/TS template literals) and multi-char
        //    delimiters may span lines; plain quotes stay single-line and
        //    honor backslash escapes.
        stringDelimiters.forEach { delim ->
            val escaped = Regex.escape(delim)
            val pattern = if (delim == "`" || delim.length > 1) {
                Regex("$escaped[\\s\\S]*?$escaped")
            } else {
                Regex("$escaped(?:[^$escaped\\\\\\n]|\\\\.)*$escaped")
            }
            rules.add(TokenRule(pattern, stringStyle))
        }

        // Raw strings (""" ... """) rank right after normal strings so any
        // accidental quote matches inside them are overridden.
        rawStringDelimiters.forEach { delim ->
            val escaped = Regex.escape(delim)
            rules.add(TokenRule(Regex("$escaped[\\s\\S]*?$escaped"), stringStyle))
        }

        // 6. Language-specific extra rules (highest priority)
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
        blockCommentPairs = listOf("/*" to "*/"),
        rawStringDelimiters = listOf("\"\"\""),
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
        rawStringDelimiters = listOf("\"\"\"", "'''"),
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
            TokenRule(Regex("(?m)^\\s*\\w+\\s*\\(\\)"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5)))
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
        blockCommentPairs = listOf("/*" to "*/"),
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
        blockCommentPairs = listOf("/*" to "*/"),
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
        singleLineComment = "--",
        keywordIgnoreCase = true
    ),
    "config" to LanguageGrammar(
        keywords = setOf("true", "false", "yes", "no", "on", "off"),
        singleLineComment = "#",
        extraRules = listOf(
            TokenRule(Regex("(?m)^\\s*\\[[^\\]]+\\]"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5))),
            TokenRule(Regex("(?m)^\\s*[\\w.-]+\\s*[=:]"), SpanStyle(color = Color(0xFFAB47BC)))
        )
    ),
    "yaml" to LanguageGrammar(
        keywords = setOf("true", "false", "yes", "no", "on", "off", "null"),
        singleLineComment = "#",
        extraRules = listOf(
            TokenRule(Regex("(?m)^\\s*[\\w.-]+\\s*:"), SpanStyle(color = Color(0xFFAB47BC)))
        )
    ),
    "json" to LanguageGrammar(
        keywords = setOf("true", "false", "null"),
        extraRules = listOf(
            TokenRule(Regex("\"[^\"]*\"\\s*:"), SpanStyle(color = Color(0xFFAB47BC)))
        )
    ),
    "markdown" to LanguageGrammar(
        keywords = emptySet(),
        extraRules = listOf(
            // Headers: # / ## / ### ... at line start
            TokenRule(Regex("(?m)^#{1,6}\\s.*$"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5))),
            // Fenced code blocks ```...```
            TokenRule(Regex("(?s)```.*?```"), SpanStyle(color = Color(0xFF66BB6A))),
            // Inline code `...`
            TokenRule(Regex("`[^`\\n]+`"), SpanStyle(color = Color(0xFF66BB6A))),
            // Bold **text** or __text__
            TokenRule(Regex("(\\*\\*[^*\\n]+\\*\\*)|(__[^_\\n]+__)"), SpanStyle(fontWeight = FontWeight.Bold)),
            // Italic *text* or _text_
            TokenRule(Regex("(?<!\\*)\\*[^*\\n]+\\*(?!\\*)|(?<!_)_[^_\\n]+_(?!_)"), SpanStyle(fontStyle = FontStyle.Italic)),
            // List markers: -, *, + or numbered at line start
            TokenRule(Regex("(?m)^\\s*([-*+]|\\d+\\.)\\s"), SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFAB47BC))),
            // Links [text](url)
            TokenRule(Regex("\\[[^\\]\\n]*\\]\\([^)\\n]*\\)"), SpanStyle(color = Color(0xFFFFA726))),
            // Blockquotes
            TokenRule(Regex("(?m)^>\\s.*$"), SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF6D6D6D)))
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
        blockCommentPairs = listOf("/*" to "*/"),
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

    /**
     * Compiled rule lists per normalized language name. Building a Regex is
     * expensive and highlight() runs on every editor keystroke, so each
     * grammar's rules are compiled once and reused for the app's lifetime.
     */
    private val rulesCache = ConcurrentHashMap<String, List<TokenRule>>()

    fun highlight(code: String, language: String?): androidx.compose.ui.text.AnnotatedString {
        if (language == null || code.isBlank()) {
            return buildAnnotatedString { append(code) }
        }

        val key = language.lowercase()
        val normalized = if (languageGrammars.containsKey(key)) key else normalizeLanguage(key)
        val grammar = languageGrammars[normalized]
            ?: return buildAnnotatedString { append(code) }

        val rules = rulesCache.getOrPut(normalized) {
            grammar.buildRules(keywordStyle, commentStyle, stringStyle, numberStyle)
        }

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
        "md" -> "markdown"
        else -> lang.lowercase()
    }
}
