package com.darknote.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.darknote.android.ui.navigation.DarkNoteNavHost
import com.darknote.android.ui.theme.DarkNoteTheme
import com.darknote.core.data.DemoDataInitializer
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.storage.FileStorageService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var snippetRepository: SnippetRepository
    @Inject lateinit var folderRepository: FolderRepository
    @Inject lateinit var storageService: FileStorageService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val demoDataInitializer = DemoDataInitializer(
                folderRepository = folderRepository,
                snippetRepository = snippetRepository,
                fileStorageService = storageService
            )
            demoDataInitializer.initializeIfEmpty()
        }

        setContent {
            DarkNoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    DarkNoteNavHost()
                }
            }
        }
    }
}