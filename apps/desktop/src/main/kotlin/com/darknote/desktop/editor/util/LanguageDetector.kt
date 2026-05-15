package com.darknote.desktop.editor.util

/**
 * Utility for detecting programming language from file content or metadata.
 * 
 * Detection strategies:
 * 1. File extension (if available)
 * 2. Shebang line (#!)
 * 3. Content analysis (keywords, patterns)
 */
object LanguageDetector {
    
    /**
     * Detects language from file name/extension.
     * 
     * @param fileName Name or path of the file
     * @return Language identifier, or null if not detected
     */
    fun detectFromFileName(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        
        return when (extension) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "rs" -> "rust"
            "go" -> "go"
            "c" -> "c"
            "cpp", "cc", "cxx", "hpp", "h" -> "cpp"
            "json" -> "json"
            "xml" -> "xml"
            "html", "htm" -> "html"
            "sql" -> "sql"
            "md", "markdown" -> "markdown"
            "sh", "bash" -> "bash"
            "yml", "yaml" -> "yaml"
            "toml" -> "toml"
            "rb" -> "ruby"
            "php" -> "php"
            "swift" -> "swift"
            "cs" -> "csharp"
            "r" -> "r"
            "scala" -> "scala"
            else -> null
        }
    }
    
    /**
     * Detects language from file content.
     * 
     * @param content File content
     * @return Language identifier, or null if not detected
     */
    fun detectFromContent(content: String): String? {
        if (content.isBlank()) return null
        
        val firstLine = content.lines().firstOrNull() ?: return null
        
        // Check shebang
        if (firstLine.startsWith("#!")) {
            return when {
                "python" in firstLine -> "python"
                "node" in firstLine || "javascript" in firstLine -> "javascript"
                "bash" in firstLine || "sh" in firstLine -> "bash"
                "ruby" in firstLine -> "ruby"
                "php" in firstLine -> "php"
                else -> null
            }
        }
        
        // Check for language-specific patterns
        return when {
            // JSON
            content.trimStart().startsWith("{") && 
            content.trim().endsWith("}") &&
            content.contains("\"") -> "json"
            
            // XML/HTML
            content.trimStart().startsWith("<") &&
            content.contains("</") -> "xml"
            
            // Kotlin
            content.contains(Regex("\\bfun\\s+\\w+")) ||
            content.contains(Regex("\\bval\\s+\\w+")) ||
            content.contains("package ") -> "kotlin"
            
            // Java
            content.contains(Regex("\\bpublic\\s+class\\s+\\w+")) ||
            content.contains(Regex("\\bprivate\\s+\\w+\\s+\\w+")) -> "java"
            
            // Python
            content.contains(Regex("\\bdef\\s+\\w+\\(")) ||
            content.contains("import ") && content.contains("from ") -> "python"
            
            // JavaScript/TypeScript
            content.contains(Regex("\\bfunction\\s+\\w+")) ||
            content.contains(Regex("\\bconst\\s+\\w+\\s*=")) ||
            content.contains("=>") -> "javascript"
            
            // Rust
            content.contains(Regex("\\bfn\\s+\\w+")) ||
            content.contains("use ") && content.contains("::") -> "rust"
            
            // Go
            content.contains(Regex("\\bfunc\\s+\\w+")) ||
            content.contains("package main") -> "go"
            
            // SQL
            content.contains(Regex("\\bSELECT\\s+", RegexOption.IGNORE_CASE)) ||
            content.contains(Regex("\\bCREATE\\s+TABLE\\s+", RegexOption.IGNORE_CASE)) -> "sql"
            
            // Markdown
            content.contains(Regex("^#+\\s+", RegexOption.MULTILINE)) ||
            content.contains("```") -> "markdown"
            
            else -> null
        }
    }
    
    /**
     * Detects language using all available information.
     * 
     * @param fileName Optional file name
     * @param content File content
     * @return Language identifier, or null if not detected
     */
    fun detect(fileName: String? = null, content: String = ""): String? {
        // Try file name first (most reliable)
        fileName?.let { name ->
            detectFromFileName(name)?.let { return it }
        }
        
        // Fall back to content analysis
        return detectFromContent(content)
    }
}
