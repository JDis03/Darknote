package com.darknote.core.model

import kotlinx.serialization.Serializable

/**
 * User settings for clipboard sanitization.
 */
@Serializable
data class ClipboardSettings(
    val autoSanitize: Boolean = true,      // Always sanitize on copy
    val removeHtml: Boolean = true,        // Remove HTML tags
    val normalizeNewlines: Boolean = true, // Convert to Unix newlines
    val trimWhitespace: Boolean = true,    // Trim leading/trailing whitespace
    val removeZeroWidth: Boolean = true    // Remove zero-width characters
) {
    companion object {
        val DEFAULT = ClipboardSettings()
    }
}
