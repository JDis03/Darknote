package com.darknote.clipboard

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

object ClipboardManager {
    /**
     * Obtiene el texto del portapapeles, lo limpia de caracteres problemáticos (\r, etc.)
     * y lo vuelve a colocar en el portapapeles como texto plano.
     */
    fun sanitize() {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                val content = clipboard.getData(DataFlavor.stringFlavor) as String
                val sanitized = cleanText(content)
                
                // Si el texto cambió, lo actualizamos en el portapapeles
                if (content != sanitized) {
                    val selection = StringSelection(sanitized)
                    clipboard.setContents(selection, null)
                    println("Portapapeles sanitizado.")
                }
            }
        } catch (e: Exception) {
            System.err.println("Error al acceder al portapapeles: ${e.message}")
        }
    }

    private fun cleanText(text: String): String {
        return text
            .replace("\r\n", "\n") // Normalizar saltos de línea
            .replace("\r", "\n")
            .filter { it.code >= 32 || it == '\n' || it == '\t' } // Eliminar caracteres de control excepto básicos
            .trim()
    }
}
