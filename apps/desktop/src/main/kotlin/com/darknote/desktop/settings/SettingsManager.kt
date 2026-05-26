package com.darknote.desktop.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

/**
 * Theme mode options.
 */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM  // Follow KDE Breeze or system theme
}

/**
 * Application settings stored in ~/.config/darknote/settings.json
 */
@Serializable
data class AppSettings(
    val themeMode: String = "system"  // "dark", "light", "system"
) {
    fun getThemeMode(): ThemeMode = when (themeMode.lowercase()) {
        "dark" -> ThemeMode.DARK
        "light" -> ThemeMode.LIGHT
        else -> ThemeMode.SYSTEM
    }
}

/**
 * Manages persistent application settings.
 */
class SettingsManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val settingsDir = File(System.getProperty("user.home"), ".config/darknote")
    private val settingsFile = File(settingsDir, "settings.json")

    init {
        // Ensure settings directory exists
        if (!settingsDir.exists()) {
            settingsDir.mkdirs()
        }
    }

    /**
     * Load settings from disk. Returns default settings if file doesn't exist.
     */
    fun load(): AppSettings {
        return try {
            if (settingsFile.exists()) {
                val content = settingsFile.readText()
                json.decodeFromString<AppSettings>(content)
            } else {
                AppSettings()
            }
        } catch (e: Exception) {
            println("Failed to load settings: ${e.message}")
            AppSettings()
        }
    }

    /**
     * Save settings to disk.
     */
    fun save(settings: AppSettings) {
        try {
            val content = json.encodeToString(settings)
            settingsFile.writeText(content)
        } catch (e: Exception) {
            println("Failed to save settings: ${e.message}")
        }
    }

    /**
     * Update theme mode and save.
     */
    fun setThemeMode(mode: ThemeMode) {
        val current = load()
        val updated = current.copy(
            themeMode = when (mode) {
                ThemeMode.DARK -> "dark"
                ThemeMode.LIGHT -> "light"
                ThemeMode.SYSTEM -> "system"
            }
        )
        save(updated)
    }
}
