package com.darknote.android.test

import com.darknote.core.clipboard.ClipboardSanitizer
import com.darknote.core.model.ClipboardSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ClipboardSanitizerTest {

    private val sanitizer = ClipboardSanitizer(ClipboardSettings.DEFAULT)

    @Test
    fun `plain text passes through unchanged`() {
        val input = "echo hello world"
        assertEquals(input, sanitizer.sanitize(input))
    }

    @Test
    fun `removes HTML tags`() {
        val input = "<b>bold</b> <span>text</span>"
        assertEquals("bold text", sanitizer.sanitize(input))
    }

    @Test
    fun `normalizes Windows newlines to Unix`() {
        val input = "line1\r\nline2\r\nline3"
        assertEquals("line1\nline2\nline3", sanitizer.sanitize(input))
    }

    @Test
    fun `normalizes old Mac newlines to Unix`() {
        val input = "line1\rline2\rline3"
        assertEquals("line1\nline2\nline3", sanitizer.sanitize(input))
    }

    @Test
    fun `removes zero-width characters`() {
        val input = "hello\u200Bworld\u200Ctest\u200Dend\uFEFF"
        assertEquals("helloworldtestend", sanitizer.sanitize(input))
    }

    @Test
    fun `replaces non-breaking spaces`() {
        val input = "hello\u00A0world"
        assertEquals("hello world", sanitizer.sanitize(input))
    }

    @Test
    fun `trims whitespace`() {
        val input = "  hello  \n  world  "
        assertEquals("hello  \n  world", sanitizer.sanitize(input))
    }

    @Test
    fun `respects autoSanitize false`() {
        val noAutoSettings = ClipboardSettings(
            autoSanitize = false,
            removeHtml = true,
            normalizeNewlines = true,
            removeZeroWidth = true,
            trimWhitespace = true
        )
        val noAutoSanitizer = ClipboardSanitizer(noAutoSettings)
        val input = "<b>hello</b>\r\n"
        assertEquals(input, noAutoSanitizer.sanitize(input))
    }

    @Test
    fun `needsSanitization detects HTML`() {
        assertTrue(sanitizer.needsSanitization("<p>hello</p>"))
    }

    @Test
    fun `needsSanitization detects Windows newlines`() {
        assertTrue(sanitizer.needsSanitization("line1\r\nline2"))
    }

    @Test
    fun `needsSanitization detects nbsp`() {
        assertTrue(sanitizer.needsSanitization("hello\u00A0world"))
    }

    @Test
    fun `needsSanitization detects zero-width space`() {
        assertTrue(sanitizer.needsSanitization("hello\u200Bworld"))
    }

    @Test
    fun `needsSanitization detects untrimmed whitespace`() {
        assertTrue(sanitizer.needsSanitization("  hello  "))
    }

    @Test
    fun `needsSanitization returns false for clean text`() {
        assertFalse(sanitizer.needsSanitization("echo hello world"))
    }

    @Test
    fun `multiline code block is preserved`() {
        val input = """#!/bin/bash
echo "hello"
if [ -f /tmp/test ]; then
    rm /tmp/test
fi"""
        assertEquals(input, sanitizer.sanitize(input))
    }

    @Test
    fun `mixed HTML and newlines handled correctly`() {
        val input = "<pre>line1\r\nline2</pre>"
        assertEquals("line1\nline2", sanitizer.sanitize(input))
    }
}
