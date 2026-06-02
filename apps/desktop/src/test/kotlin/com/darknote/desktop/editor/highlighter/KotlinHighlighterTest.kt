package com.darknote.desktop.editor.highlighter

import com.darknote.desktop.editor.theme.EditorColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for KotlinHighlighter
 */
class KotlinHighlighterTest {

    private lateinit var highlighter: KotlinHighlighter
    private lateinit var colorScheme: EditorColorScheme

    @Before
    fun setup() {
        highlighter = KotlinHighlighter()
        colorScheme = EditorColorScheme.DARK
    }

    @Test
    fun `test keyword highlighting`() {
        val code = "fun main() {}"
        val result = highlighter.highlight(code, colorScheme)

        // Result should be an AnnotatedString containing the code
        assertEquals("Should contain the code", code, result.text)
        
        // Should have span annotations for styling
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test class keyword highlighting`() {
        val code = "class MyClass"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test string literal highlighting`() {
        val code = "val text = \"Hello World\""
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test single line comment highlighting`() {
        val code = "// This is a comment"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test multi-line comment highlighting`() {
        val code = "/* multi\nline */\nval x = 1"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test number highlighting`() {
        val code = "val x = 42"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test annotation highlighting`() {
        val code = "@Test fun test() {}"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test empty code returns empty string`() {
        val result = highlighter.highlight("", colorScheme)
        assertEquals("Empty code should return empty string", "", result.text)
    }

    @Test
    fun `test whitespace only code`() {
        val code = "   \n\t "
        val result = highlighter.highlight(code, colorScheme)
        
        assertEquals("Whitespace should be preserved", code, result.text)
    }

    @Test
    fun `test complex code snippet`() {
        val code = """
            @Composable
            fun MyScreen(name: String) {
                // Display name
                Text("Hello")
            }
        """.trimIndent()

        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should preserve all text", code, result.text)
        assertTrue("Should have multiple style annotations", result.spanStyles.size > 3)
    }

    @Test
    fun `test different color schemes produce same text`() {
        val code = "fun test() {}"
        
        val darculaResult = highlighter.highlight(code, EditorColorScheme.DARK)
        val lightResult = highlighter.highlight(code, EditorColorScheme.LIGHT)
        val vscodeResult = highlighter.highlight(code, EditorColorScheme.VS_CODE_DARK)

        // All should have same text
        assertEquals("All color schemes should preserve text", 
            darculaResult.text, lightResult.text)
        assertEquals("All color schemes should preserve text", 
            darculaResult.text, vscodeResult.text)
            
        // All should have styling
        assertTrue("Darcula should have styling", darculaResult.spanStyles.isNotEmpty())
        assertTrue("Light should have styling", lightResult.spanStyles.isNotEmpty())
        assertTrue("VSCode should have styling", vscodeResult.spanStyles.isNotEmpty())
    }
}
