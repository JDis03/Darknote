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
import com.darknote.desktop.clipboard.DesktopClipboardManager
import com.darknote.core.clipboard.ClipboardSanitizer
import com.darknote.core.model.ClipboardSettings

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
    var copiedText by remember { mutableStateOf<String?>(null) }
    val clipboardManager = remember {
        DesktopClipboardManager(ClipboardSanitizer(ClipboardSettings.DEFAULT))
    }

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
            // Sidebar
            Box(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Text(
                    "Snippet Tree\n\n- Scripts\n- Configs\n- Commands",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Divider(
                modifier = Modifier.fillMaxHeight().width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Main content
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
                        "Fase 2: Core y Domain ✅",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "Clipboard Sanitizer funcionando",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // Demo de clipboard sanitizado
                    var inputText by remember { mutableStateOf("curl\\r\\nhttp://example.com") }
                    
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Texto a copiar") },
                        modifier = Modifier.width(400.dp)
                    )
                    
                    Button(
                        onClick = {
                            clipboardManager.copy(inputText, sanitize = true)
                            copiedText = "Copiado (sanitizado): ${inputText.take(50)}..."
                        }
                    ) {
                        Text("📋 Copiar Sanitizado")
                    }
                    
                    copiedText?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
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
