package com.darknote.android.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object SnippetDetail : Screen("snippet/{snippetId}") {
        fun createRoute(snippetId: String) = "snippet/$snippetId"
    }
    data object CreateSnippet : Screen("create")
    data object EditSnippet : Screen("edit/{snippetId}") {
        fun createRoute(snippetId: String) = "edit/$snippetId"
    }
    data object Settings : Screen("settings")
}