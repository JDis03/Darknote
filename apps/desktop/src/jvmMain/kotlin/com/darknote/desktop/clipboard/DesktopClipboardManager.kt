package com.darknote.desktop.clipboard

import com.darknote.core.clipboard.ClipboardManager
import com.darknote.core.clipboard.ClipboardSanitizer
import com.darknote.core.model.ClipboardSettings
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

class DesktopClipboardManager(
    sanitizer: ClipboardSanitizer
) : ClipboardManager(sanitizer) {

    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard

    override fun copy(text: String, sanitize: Boolean) {
        val finalText = if (sanitize) sanitizeText(text) else text
        val selection = StringSelection(finalText)
        clipboard.setContents(selection, selection)
    }

    override fun paste(): String? {
        return try {
            clipboard.getData(DataFlavor.stringFlavor) as? String
        } catch (e: Exception) { null }
    }

    override fun hasText(): Boolean {
        return try {
            clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
        } catch (e: Exception) { false }
    }
}

class DesktopClipboardManagerFactory {
    fun create(settings: ClipboardSettings = ClipboardSettings.DEFAULT): ClipboardManager {
        return DesktopClipboardManager(ClipboardSanitizer(settings))
    }
}
