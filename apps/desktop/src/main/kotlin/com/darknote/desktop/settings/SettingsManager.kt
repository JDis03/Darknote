package com.darknote.desktop.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Theme mode options. */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM  // Follow KDE Breeze or system theme
}

/**
 * Application settings stored in ~/.config/darknote/settings.json.
 *
 * `themeMode` is intentionally stored as a lowercase String (not the enum) for
 * forward/backward compatibility and to tolerate unknown values gracefully.
 * Use [themeModeEnum] to read the typed enum.
 */
@Serializable
data class AppSettings(
    val themeMode: String = DEFAULT_THEME
) {
    /** Typed enum view of [themeMode]. Unknown values map to [ThemeMode.SYSTEM]. */
    val themeModeEnum: ThemeMode
        get() = when (themeMode.lowercase()) {
            "dark" -> ThemeMode.DARK
            "light" -> ThemeMode.LIGHT
            else -> ThemeMode.SYSTEM
        }

    companion object {
        const val DEFAULT_THEME = "system"
        val DEFAULT = AppSettings()
    }
}

/**
 * Manages persistent application settings.
 */
open class SettingsManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true  // explicit, even though "system" is default
    }

    /** Override to use a custom directory (tests). */
    protected open fun resolveDir(): File = File(System.getProperty("user.home"), ".config/darknote")

    private val settingsDir: File by lazy { resolveDir().also { if (!it.exists()) it.mkdirs() } }
    private val settingsFile: File by lazy { File(settingsDir, "settings.json") }

    fun load(): AppSettings = runCatching {
        if (settingsFile.exists()) {
            json.decodeFromString<AppSettings>(settingsFile.readText())
        } else {
            AppSettings.DEFAULT
        }
    }.getOrElse { AppSettings.DEFAULT }

    /** @return true if persisted, false on disk error. */
    fun save(settings: AppSettings): Boolean = runCatching {
        settingsFile.writeText(json.encodeToString(settings))
        true
    }.getOrDefault(false)

    /** @return true if persisted, false on disk error. */
    fun setThemeMode(mode: ThemeMode): Boolean {
        val key = when (mode) {
            ThemeMode.DARK -> "dark"
            ThemeMode.LIGHT -> "light"
            ThemeMode.SYSTEM -> AppSettings.DEFAULT_THEME
        }
        return save(load().copy(themeMode = key))
    }
}
