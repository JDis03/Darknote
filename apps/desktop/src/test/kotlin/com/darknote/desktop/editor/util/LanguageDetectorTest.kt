package com.darknote.desktop.editor.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for LanguageDetector
 */
class LanguageDetectorTest {

    @Test
    fun `test detect Kotlin by extension`() {
        assertEquals("kotlin", LanguageDetector.detect("Main.kt", ""))
    }

    @Test
    fun `test detect Java by extension`() {
        assertEquals("java", LanguageDetector.detect("Main.java", ""))
    }

    @Test
    fun `test detect Python by extension`() {
        assertEquals("python", LanguageDetector.detect("script.py", ""))
    }

    @Test
    fun `test detect JavaScript by extension`() {
        assertEquals("javascript", LanguageDetector.detect("app.js", ""))
    }

    @Test
    fun `test detect TypeScript by extension`() {
        assertEquals("typescript", LanguageDetector.detect("app.ts", ""))
    }

    @Test
    fun `test detect Rust by extension`() {
        assertEquals("rust", LanguageDetector.detect("main.rs", ""))
    }

    @Test
    fun `test detect Go by extension`() {
        assertEquals("go", LanguageDetector.detect("main.go", ""))
    }

    @Test
    fun `test detect C++ by cpp extension`() {
        assertEquals("cpp", LanguageDetector.detect("main.cpp", ""))
    }

    @Test
    fun `test detect C++ by cc extension`() {
        assertEquals("cpp", LanguageDetector.detect("main.cc", ""))
    }

    @Test
    fun `test detect C++ by cxx extension`() {
        assertEquals("cpp", LanguageDetector.detect("main.cxx", ""))
    }

    @Test
    fun `test detect JSON by extension`() {
        assertEquals("json", LanguageDetector.detect("config.json", ""))
    }

    @Test
    fun `test detect XML by extension`() {
        assertEquals("xml", LanguageDetector.detect("pom.xml", ""))
    }

    @Test
    fun `test detect SQL by extension`() {
        assertEquals("sql", LanguageDetector.detect("schema.sql", ""))
    }

    @Test
    fun `test detect Markdown by md extension`() {
        assertEquals("markdown", LanguageDetector.detect("README.md", ""))
    }

    @Test
    fun `test detect Markdown by markdown extension`() {
        assertEquals("markdown", LanguageDetector.detect("docs.markdown", ""))
    }

    @Test
    fun `test detect Kotlin by content with package declaration`() {
        val content = "package com.example\n\nfun main() {}"
        assertEquals("kotlin", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect Kotlin by content with fun keyword`() {
        val content = "fun main() {\n    println(\"hello\")\n}"
        assertEquals("kotlin", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect Java by content with public class`() {
        val content = "public class Main {\n    public static void main(String[] args) {}\n}"
        assertEquals("java", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect Python by content with def`() {
        val content = "def main():\n    print('hello')"
        assertEquals("python", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect JavaScript by content with function`() {
        val content = "function test() {\n    console.log('test');\n}"
        assertEquals("javascript", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect JavaScript by content with const`() {
        val content = "const x = 5;\nconst y = 10;"
        assertEquals("javascript", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect Rust by content with fn`() {
        val content = "fn main() {\n    println!(\"Hello\");\n}"
        assertEquals("rust", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect Go by content with func`() {
        val content = "func main() {\n    fmt.Println(\"Hello\")\n}"
        assertEquals("go", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect JSON by content with object`() {
        val content = "{\n  \"name\": \"test\",\n  \"version\": \"1.0\"\n}"
        assertEquals("json", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect XML by content with xml tag`() {
        val content = "<?xml version=\"1.0\"?>\n<root></root>"
        assertEquals("xml", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect SQL by content with SELECT`() {
        val content = "SELECT * FROM users WHERE id = 1;"
        assertEquals("sql", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect SQL by content with CREATE TABLE`() {
        val content = "CREATE TABLE users (id INT PRIMARY KEY);"
        assertEquals("sql", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect Markdown by content with heading`() {
        val content = "# Title\n\nSome text"
        assertEquals("markdown", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test detect Markdown by content with code blocks`() {
        val content = "```\nsome code\n```"
        assertEquals("markdown", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test extension takes precedence over content`() {
        val pythonContent = "def main():\n    pass"
        assertEquals("kotlin", LanguageDetector.detect("test.kt", pythonContent))
    }

    @Test
    fun `test unknown extension with unknown content returns null`() {
        assertNull(LanguageDetector.detect("test.xyz", "random content"))
    }

    @Test
    fun `test empty filename and empty content returns null`() {
        assertNull(LanguageDetector.detect("", ""))
    }

    @Test
    fun `test case insensitive extension matching`() {
        assertEquals("kotlin", LanguageDetector.detect("Main.KT", ""))
        assertEquals("java", LanguageDetector.detect("Main.JAVA", ""))
        assertEquals("python", LanguageDetector.detect("script.PY", ""))
    }

    @Test
    fun `test filename with multiple dots`() {
        assertEquals("kotlin", LanguageDetector.detect("my.test.file.kt", ""))
        assertEquals("json", LanguageDetector.detect("package.lock.json", ""))
    }

    @Test
    fun `test shebang detection for Python`() {
        val content = "#!/usr/bin/env python3\nprint('hello')"
        assertEquals("python", LanguageDetector.detect(null, content))
    }

    @Test
    fun `test all supported languages are detectable`() {
        val supportedLanguages = listOf(
            "kotlin", "java", "python", "javascript", "typescript",
            "rust", "go", "cpp", "json", "xml", "sql", "markdown"
        )

        val extensions = mapOf(
            "kotlin" to "kt",
            "java" to "java",
            "python" to "py",
            "javascript" to "js",
            "typescript" to "ts",
            "rust" to "rs",
            "go" to "go",
            "cpp" to "cpp",
            "json" to "json",
            "xml" to "xml",
            "sql" to "sql",
            "markdown" to "md"
        )

        supportedLanguages.forEach { lang ->
            val ext = extensions[lang]!!
            val detected = LanguageDetector.detect("test.$ext", "")
            assertEquals("Should detect $lang by extension", lang, detected)
        }
    }
    
    @Test
    fun `test detectFromFileName method`() {
        assertEquals("kotlin", LanguageDetector.detectFromFileName("Main.kt"))
        assertEquals("python", LanguageDetector.detectFromFileName("script.py"))
        assertNull(LanguageDetector.detectFromFileName("unknown.xyz"))
    }
    
    @Test
    fun `test detectFromContent method`() {
        val kotlinContent = "fun main() {}"
        assertEquals("kotlin", LanguageDetector.detectFromContent(kotlinContent))
        
        val pythonContent = "def main():\n    pass"
        assertEquals("python", LanguageDetector.detectFromContent(pythonContent))
        
        assertNull(LanguageDetector.detectFromContent("random text"))
    }
}
