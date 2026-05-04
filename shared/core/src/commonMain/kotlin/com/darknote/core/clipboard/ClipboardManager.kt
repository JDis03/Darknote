package com.darknote.core.clipboard

import com.darknote.core.model.ClipboardSettings

/**
 * Factory interface for creating platform-specific ClipboardManager instances.
 * Each platform provides its own implementation.
 */
interface ClipboardManagerFactory {
    fun create(settings: ClipboardSettings = ClipboardSettings.DEFAULT): ClipboardManager
}

/**
 * Abstract base class for ClipboardManager.
 * Platform implementations should extend this.
 */
abstract class ClipboardManager(
    protected val sanitizer: ClipboardSanitizer
) {
    /**
     * Copy text to system clipboard.
     * @param text Text to copy
     * @param sanitize Whether to sanitize text for terminal
     */
    abstract fun copy(text: String, sanitize: Boolean = true)

    /**
     * Get text from system clipboard.
     */
    abstract fun paste(): String?

    /**
     * Check if clipboard has text content.
     */
    abstract fun hasText(): Boolean

    /**
     * Copy with sanitization applied.
     */
    protected fun sanitizeText(text: String): String {
        return sanitizer.sanitize(text)
    }
}
