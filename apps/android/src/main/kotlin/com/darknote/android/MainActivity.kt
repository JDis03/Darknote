package com.darknote.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.darknote.android.clipboard.AndroidClipboardManager
import com.darknote.core.clipboard.ClipboardSanitizer
import com.darknote.core.data.DemoDataInitializer
import com.darknote.core.model.ClipboardSettings
import com.darknote.core.model.Snippet
import com.darknote.core.storage.FileStorageService
import com.darknote.persistence.database.AndroidDriverFactory
import com.darknote.persistence.database.DatabaseFactory
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storageDir = File(filesDir, "snippets").apply { mkdirs() }
        val storageService = FileStorageService(storageDir.parentFile!!)
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

        lifecycleScope.launch {
            val demoDataInitializer = DemoDataInitializer(
                folderRepository = databaseFactory.folderRepository,
                snippetRepository = databaseFactory.snippetRepository,
                fileStorageService = storageService
            )
            demoDataInitializer.initializeIfEmpty()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SnippetListScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetListScreen(viewModel: SnippetListViewModel) {
    val snippets by viewModel.filteredSnippets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val copiedSnippetId by viewModel.copiedSnippetId.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DarkNote") },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowFavorites() }) {
                        Icon(
                            imageVector = if (viewModel.showFavoritesOnly.value) 
                                Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Favorites"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Create new snippet - simplified */ }
            ) {
                Icon(Icons.Default.Add, "Add snippet")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text("Buscar snippets...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )
            
            // Snippet list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(snippets, key = { it.id }) { snippet ->
                    val isCopied = copiedSnippetId == snippet.id
                    SnippetCard(
                        snippet = snippet,
                        isCopied = isCopied,
                        onCopy = { viewModel.copySnippet(snippet) },
                        onToggleFavorite = { viewModel.toggleFavorite(snippet) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetCard(
    snippet: Snippet,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCopy,
        colors = CardDefaults.cardColors(
            containerColor = if (isCopied) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = snippet.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (snippet.isFavorite) 
                                Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Favorite",
                            tint = if (snippet.isFavorite) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = if (isCopied) 
                                Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy"
                        )
                    }
                }
            }
            
            if (snippet.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    snippet.tags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            
            Text(
                text = snippet.content.take(100) + if (snippet.content.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}