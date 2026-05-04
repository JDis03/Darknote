package com.darknote.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.darknote.android.clipboard.AndroidClipboardManager
import com.darknote.android.ui.navigation.DarkNoteNavHost
import com.darknote.android.ui.theme.DarkNoteTheme
import com.darknote.core.clipboard.ClipboardSanitizer
import com.darknote.core.data.DemoDataInitializer
import com.darknote.core.model.ClipboardSettings
import com.darknote.core.storage.FileStorageService
import com.darknote.persistence.database.AndroidDriverFactory
import com.darknote.persistence.database.DatabaseFactory
import com.darknote.sync.client.DropboxClientFactory
import com.darknote.android.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    
    private lateinit var authViewModel: AuthViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Dropbox client factory (uses same app key as Desktop)
        DropboxClientFactory.initialize(this)

        val storageDir = File(filesDir, "snippets").apply { mkdirs() }
        val storageService = FileStorageService(storageDir)
        val databaseFactory = DatabaseFactory(AndroidDriverFactory(this))
        val clipboardManager = AndroidClipboardManager(
            ClipboardSanitizer(ClipboardSettings.DEFAULT),
            this
        )

        val viewModel = SnippetListViewModel(
            snippetRepository = databaseFactory.snippetRepository,
            folderRepository = databaseFactory.folderRepository,
            storageService = storageService,
            clipboardManager = clipboardManager
        )

        authViewModel = AuthViewModel(
            dropboxClient = DropboxClientFactory.create()
        )

        lifecycleScope.launch {
            val demoDataInitializer = DemoDataInitializer(
                folderRepository = databaseFactory.folderRepository,
                snippetRepository = databaseFactory.snippetRepository,
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
                    DarkNoteNavHost(
                        viewModel = viewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
        
        // Handle OAuth callback if this is from a redirect
        handleOAuthCallback(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }
    
    private fun handleOAuthCallback(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null && data.scheme == "db-97rske3f4p28pex") {
            val code = data.getQueryParameter("code")
            if (code != null) {
                authViewModel.completeAuth(code)
            }
        }
    }
}