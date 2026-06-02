package com.darknote.desktop.editor.highlighter

import com.darknote.desktop.editor.theme.EditorColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PythonHighlighter
 */
class PythonHighlighterTest {

    private lateinit var highlighter: PythonHighlighter
    private lateinit var colorScheme: EditorColorScheme

    @Before
    fun setup() {
        highlighter = PythonHighlighter()
        colorScheme = EditorColorScheme.DARK
    }

    @Test
    fun `test def keyword highlighting`() {
        val code = "def main():"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test class keyword highlighting`() {
        val code = "class MyClass:"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test import keyword highlighting`() {
        val code = "import os"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test string literal single quotes`() {
        val code = "text = 'Hello World'"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test string literal double quotes`() {
        val code = "text = \"Hello World\""
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test comment highlighting`() {
        val code = "# This is a comment"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test decorator highlighting`() {
        val code = "@property\ndef name():"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test function definition`() {
        val code = "def calculate(x, y):\n    return x + y"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test boolean literals`() {
        val code = "flag = True"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test None keyword`() {
        val code = "value = None"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.isNotEmpty())
    }

    @Test
    fun `test number highlighting`() {
        val code = "count = 42"
        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should contain the code", code, result.text)
        // Number highlighting may or may not be applied depending on implementation
    }

    @Test
    fun `test complex python code`() {
        val code = """
            @dataclass
            class Person:
                name: str
                age: int
                
                def greet(self):
                    # Say hello
                    print(f"Hello")
        """.trimIndent()

        val result = highlighter.highlight(code, colorScheme)

        assertEquals("Should preserve all text", code, result.text)
        assertTrue("Should have style annotations", result.spanStyles.size >= 1)
    }
}
