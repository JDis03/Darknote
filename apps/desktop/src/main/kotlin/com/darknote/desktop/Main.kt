package com.darknote.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.darknote.core.model.Snippet
import com.darknote.desktop.di.desktopModule
import com.darknote.desktop.di.initKoin
import com.darknote.desktop.platform.KDEIntegration
import com.darknote.desktop.ui.screens.EditorScreen
import com.darknote.desktop.ui.screens.SnippetListScreen
import com.darknote.desktop.viewmodel.SnippetListViewModel
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import java.awt.Dimension

fun main() = application {
    // Initialize Koin
    KoinApplication(application = {
        modules(initKoin())
    }) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "DarkNote",
            state = rememberWindowState(
                width = 1200.dp,
                height = 800.dp
            )
        ) {
            window.minimumSize = Dimension(800, 600)
            
            // Use KDE Breeze theme if running in KDE, otherwise dark theme
            val colorScheme = remember {
                if (KDEIntegration.isKDE()) {
                    KDEIntegration.getKDEColorScheme()
                } else {
                    darkColorScheme()
                }
            }
            
            MaterialTheme(
                colorScheme = colorScheme
            ) {
                App()
            }
        }
    }
}

sealed class Screen {
    data object List : Screen()
    data class Editor(val snippetId: String) : Screen()
}

@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.List) }
    val viewModel: SnippetListViewModel = koinInject()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val screen = currentScreen) {
            Screen.List -> SnippetListScreen(
                viewModel = viewModel,
                onSnippetClick = { snippet ->
                    currentScreen = Screen.Editor(snippet.id)
                },
                onCreateSnippet = { snippetId ->
                    currentScreen = Screen.Editor(snippetId)
                }
            )
            is Screen.Editor -> EditorScreen(
                snippetId = screen.snippetId,
                viewModel = viewModel,
                onBack = { currentScreen = Screen.List }
            )
        }
    }
}
