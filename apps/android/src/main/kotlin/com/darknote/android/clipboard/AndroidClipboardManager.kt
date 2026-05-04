package com.darknote.android.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.ClipboardManager as AndroidSystemClipboardManager
import com.darknote.core.clipboard.ClipboardManager
import com.darknote.core.clipboard.ClipboardSanitizer
import com.darknote.core.model.ClipboardSettings

class AndroidClipboardManager(
    sanitizer: ClipboardSanitizer,
    private val context: Context
) : ClipboardManager(sanitizer) {

    private val clipboard: AndroidSystemClipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidSystemClipboardManager
    }

    override fun copy(text: String, sanitize: Boolean) {
        val finalText = if (sanitize) sanitizeText(text) else text
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

    companion object {
        fun create(context: Context, settings: ClipboardSettings = ClipboardSettings.DEFAULT): AndroidClipboardManager {
            val sanitizer = ClipboardSanitizer(settings)
            return AndroidClipboardManager(sanitizer, context)
        }
    }
}