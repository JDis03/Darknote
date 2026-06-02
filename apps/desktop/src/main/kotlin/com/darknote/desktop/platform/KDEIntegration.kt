package com.darknote.desktop.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import java.io.File

/**
 * KDE Plasma integration utilities.
 * Detects KDE environment and applies Breeze theme colors.
 */
object KDEIntegration {
    
    /**
     * Check if running in KDE Plasma environment.
     */
    fun isKDE(): Boolean {
        val desktop = System.getenv("XDG_CURRENT_DESKTOP")?.lowercase() ?: ""
        val session = System.getenv("DESKTOP_SESSION")?.lowercase() ?: ""
        
        return desktop.contains("kde") || 
               session.contains("plasma") || 
               session.contains("kde")
    }
    
    /**
     * Detect if KDE is using dark theme.
     */
    fun isKDEDarkTheme(): Boolean {
        if (!isKDE()) return true // Default to dark if not KDE
        
        // Check KDE color scheme file
        val homeDir = System.getProperty("user.home")
        val kdeConfig = File("$homeDir/.config/kdeglobals")
        
        if (kdeConfig.exists()) {
            try {
                val content = kdeConfig.readText()
                // Look for color scheme name
                if (content.contains("ColorScheme=Breeze Light", ignoreCase = true)) {
                    return false
                }
                if (content.contains("ColorScheme=Breeze Dark", ignoreCase = true)) {
                    return true
                }
                
                // Check WM color section for dark colors
                val wmSection = content.substringAfter("[WM]", "")
                    .substringBefore("[", content)
                
                if (wmSection.contains("activeBackground=")) {
                    val bg = wmSection.substringAfter("activeBackground=")
                        .substringBefore("\n")
                        .split(",")
                        .firstOrNull()?.toIntOrNull() ?: 255
                    
                    // If background color component < 128, it's dark
                    return bg < 128
                }
            } catch (e: Exception) {
                // Ignore errors, fall back to default
            }
        }
        
        return true // Default to dark
    }
    
    /**
     * Get ColorScheme matching KDE Breeze theme.
     */
    fun getKDEColorScheme(): ColorScheme {
        val isDark = isKDEDarkTheme()
        
        return if (isDark) {
            // Breeze Dark colors
            darkColorScheme(
                primary = Color(0xFF3DAEE9),           // Breeze blue
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF1E6A99),
                onPrimaryContainer = Color(0xFFFFFFFF),
                
                secondary = Color(0xFF93CEE8),
                onSecondary = Color(0xFF000000),
                secondaryContainer = Color(0xFF31363B),
                onSecondaryContainer = Color(0xFFEFF0F1),
                
                tertiary = Color(0xFF2ECC71),          // Breeze green
                onTertiary = Color(0xFF000000),
                
                error = Color(0xFFDA4453),             // Breeze red
                onError = Color(0xFFFFFFFF),
                
                background = Color(0xFF232629),        // Breeze Dark background
                onBackground = Color(0xFFEFF0F1),      // Breeze Dark foreground
                
                surface = Color(0xFF31363B),           // Breeze Dark surface
                onSurface = Color(0xFFEFF0F1),
                
                surfaceVariant = Color(0xFF3B4045),
                onSurfaceVariant = Color(0xFFBDC3C7),
                
                outline = Color(0xFF4D4D4D),
                outlineVariant = Color(0xFF2A2E32)
            )
        } else {
            // Breeze Light colors
            lightColorScheme(
                primary = Color(0xFF3DAEE9),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFBDE3F3),
                onPrimaryContainer = Color(0xFF000000),
                
                secondary = Color(0xFF93CEE8),
                onSecondary = Color(0xFF000000),
                secondaryContainer = Color(0xFFEFF0F1),
                onSecondaryContainer = Color(0xFF31363B),
                
                tertiary = Color(0xFF27AE60),
                onTertiary = Color(0xFFFFFFFF),
                
                error = Color(0xFFDA4453),
                onError = Color(0xFFFFFFFF),
                
                background = Color(0xFFFCFCFC),        // Breeze Light background
                onBackground = Color(0xFF232629),      // Breeze Light foreground
                
                surface = Color(0xFFEFF0F1),           // Breeze Light surface
                onSurface = Color(0xFF31363B),
                
                surfaceVariant = Color(0xFFE3E5E7),
                onSurfaceVariant = Color(0xFF4D4D4D),
                
                outline = Color(0xFFBDC3C7),
                outlineVariant = Color(0xFFD3D4D5)
            )
        }
    }
    
    /**
     * Get desktop file path for installation.
     */
    fun getDesktopFilePath(): String {
        val homeDir = System.getProperty("user.home")
        return "$homeDir/.local/share/applications/darknote.desktop"
    }
    
    /**
     * Install desktop entry for KDE.
     */
    fun installDesktopEntry(execPath: String): Boolean {
        val desktopFile = File(getDesktopFilePath())
        desktopFile.parentFile?.mkdirs()
        
        try {
            val content = """
                [Desktop Entry]
                Version=1.0
                Type=Application
                Name=DarkNote
                GenericName=Snippet Manager
                Comment=Modern snippet manager with Dropbox sync
                Exec=$execPath %U
                Icon=darknote
                Terminal=false
                Categories=Utility;TextEditor;Development;
                Keywords=snippet;code;clipboard;sync;dropbox;
                MimeType=text/plain;
                StartupNotify=true
                StartupWMClass=DarkNote
                X-KDE-StartupNotify=true
                
                Actions=NewSnippet;Search;
                
                [Desktop Action NewSnippet]
                Name=New Snippet
                Exec=$execPath --new
                
                [Desktop Action Search]
                Name=Search Snippets
                Exec=$execPath --search
            """.trimIndent()
            
            desktopFile.writeText(content)
            desktopFile.setExecutable(true)
            
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
