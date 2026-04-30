package com.darknote.android.clipboard

import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import android.content.ClipData
import android.content.ClipDescription
import com.darknote.core.clipboard.ClipboardManager
import com.darknote.core.clipboard.ClipboardManagerFactory
import com.darknote.core.clipboard.ClipboardSanitizer
import com.darknote.core.model.ClipboardSettings

/**
 * Android implementation of ClipboardManager.
 * Requires Context from Application or Activity.
 */
class AndroidClipboardManager(
    sanitizer: ClipboardSanitizer,
    private val context: Context
) : ClipboardManager(sanitizer) {

    private val clipboard: AndroidClipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
    }

    override fun copy(text: String, sanitize: Boolean) {
        val finalText = if (sanitize) {
            sanitizeText(text)
        } else {
            text
        }

        val clip = ClipData.newPlainText("DarkNote Snippet", finalText)
        clipboard.setPrimaryClip(clip)
    }

    override fun paste(): String? {
        return try {
            clipboard.primaryClip?.let { clip ->
                if (clip.itemCount > 0 && clip.getItemAt(0).text != null) {
                    clip.getItemAt(0).text.toString()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun hasText(): Boolean {
        return try {
            clipboard.hasPrimaryClip() &&
                    clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Android factory implementation.
 * Note: This is a placeholder. In real implementation,
 * you'd inject Context through DI or use Application context.
 */
actual class ClipboardManagerFactory actual constructor() {
    actual fun create(settings: ClipboardSettings): ClipboardManager {
        // This is a placeholder. In actual Android app,
        // Context should be injected via Koin or similar
        throw NotImplementedError("Android ClipboardManager requires Context injection")
    }

    /**
     * Create with Context - call this from Application or Activity.
     */
    fun create(context: Context, settings: ClipboardSettings = ClipboardSettings.DEFAULT): ClipboardManager {
        val sanitizer = ClipboardSanitizer(settings)
        return AndroidClipboardManager(sanitizer, context)
    }
}
