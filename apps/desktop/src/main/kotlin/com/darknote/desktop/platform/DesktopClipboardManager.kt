package com.darknote.desktop.platform

import com.darknote.core.clipboard.ClipboardManager
import com.darknote.core.clipboard.ClipboardSanitizer
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

class DesktopClipboardManager : ClipboardManager(ClipboardSanitizer()) {
    
    override fun copy(text: String, sanitize: Boolean) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val content = if (sanitize) sanitizeText(text) else text
        clipboard.setContents(StringSelection(content), null)
    }
    
    override fun paste(): String? {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                clipboard.getData(DataFlavor.stringFlavor) as? String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun hasText(): Boolean {
        return try {
            Toolkit.getDefaultToolkit().systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
        } catch (e: Exception) {
            false
        }
    }
}
