package com.darknote.desktop.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Theme mode options.
 */
@Serializable
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
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

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
        if (!settingsDir.exists()) {
            settingsDir.mkdirs()
        }
    }

    fun load(): AppSettings = runCatching {
        if (settingsFile.exists()) {
            json.decodeFromString<AppSettings>(settingsFile.readText())
        } else {
            AppSettings()
        }
    }.getOrElse { AppSettings() }

    fun save(settings: AppSettings) = runCatching {
        settingsFile.writeText(json.encodeToString(settings))
    }

    fun setThemeMode(mode: ThemeMode) = save(load().copy(themeMode = mode))
}
