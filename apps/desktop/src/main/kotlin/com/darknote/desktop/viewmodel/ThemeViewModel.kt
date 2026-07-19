package com.darknote.desktop.viewmodel

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.darknote.desktop.platform.KDEIntegration
import com.darknote.desktop.settings.SettingsManager
import com.darknote.desktop.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing application theme.
 */
class ThemeViewModel(
    private val settingsManager: SettingsManager
) {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _colorScheme = MutableStateFlow<ColorScheme>(darkColorScheme())
    val colorScheme: StateFlow<ColorScheme> = _colorScheme.asStateFlow()

    init {
        loadTheme()
    }

    private fun loadTheme() {
        _themeMode.value = settingsManager.load().themeModeEnum
        updateColorScheme()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        settingsManager.setThemeMode(mode)
        updateColorScheme()
    }

    private fun updateColorScheme() {
        _colorScheme.value = when (_themeMode.value) {
            ThemeMode.DARK -> darkColorScheme()
            ThemeMode.LIGHT -> lightColorScheme()
            ThemeMode.SYSTEM -> {
                if (KDEIntegration.isKDE()) {
                    KDEIntegration.getKDEColorScheme()
                } else {
                    darkColorScheme()
                }
            }
        }
    }
}
