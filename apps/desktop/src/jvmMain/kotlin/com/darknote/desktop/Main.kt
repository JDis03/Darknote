package com.darknote.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DarkNote - Snippet Manager"
    ) {
        MaterialTheme {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    var selectedSnippet by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DarkNote") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sidebar with tree (placeholder)
            Box(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Text(
                    "Snippet Tree\n(Árbol de carpetas y snippets)",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Divider(
                modifier = Modifier.fillMaxHeight().width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Main editor area (placeholder)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Editor de Snippets",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "Fase 1: KMP Setup ✅\n" +
                        "Texto plano puro - Sin markdown\n" +
                        "Copiar sanitizado para terminal",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { /* Copy sanitized */ }
                    ) {
                        Text("📋 Copiar (Sanitizado)")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMainScreen() {
    MaterialTheme {
        MainScreen()
    }
}
