package com.darknote.core.clipboard

import com.darknote.core.model.ClipboardSettings

/**
 * Sanitizes text for safe pasting into terminal.
 * Removes formatting that causes errors when copying from rich editors.
 */
class ClipboardSanitizer(
    private val settings: ClipboardSettings = ClipboardSettings.DEFAULT
) {
    /**
     * Sanitizes text according to current settings.
     * @param text The text to sanitize
     * @return Sanitized text safe for terminal
     */
    fun sanitize(text: String): String {
        if (!settings.autoSanitize) {
            return text
        }

        var result = text

        // Remove HTML tags
        if (settings.removeHtml) {
            result = result.replace(Regex("<[^>]*>"), "")
        }

        // Normalize newlines to Unix style
        if (settings.normalizeNewlines) {
            result = result.replace("\r\n", "\n")  // Windows
            result = result.replace("\r", "\n")    // Old Mac
        }

        // Remove zero-width characters
        if (settings.removeZeroWidth) {
            result = result.replace("\u200B", "")  // Zero-width space
            result = result.replace("\u200C", "")  // Zero-width non-joiner
            result = result.replace("\u200D", "")  // Zero-width joiner
            result = result.replace("\uFEFF", "")  // BOM
        }

        // Replace non-breaking spaces with regular spaces
        result = result.replace("\u00A0", " ")

        // Trim whitespace
        if (settings.trimWhitespace) {
            result = result.trim()
        }

        return result
    }

    /**
     * Checks if text needs sanitization.
     * Useful for showing warning to user.
     */
    fun needsSanitization(text: String): Boolean {
        return text.contains("\r") ||
               text.contains(Regex("<[^>]*>")) ||
               text.contains("\u00A0") ||
               text.contains("\u200B") ||
               text != text.trim()
    }
}
