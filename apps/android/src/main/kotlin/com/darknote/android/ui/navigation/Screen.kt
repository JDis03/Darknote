package com.darknote.android.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Editor : Screen("editor/{snippetId}") {
        fun createRoute(snippetId: String) = "editor/$snippetId"
    }
    data object Settings : Screen("settings")
}