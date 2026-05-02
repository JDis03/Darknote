package com.darknote.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.darknote.desktop.clipboard.DesktopClipboardManager
import com.darknote.core.clipboard.ClipboardSanitizer
import com.darknote.core.model.ClipboardSettings
import com.darknote.core.storage.FileStorageService
import com.darknote.desktop.data.DemoDataInitializer
import com.darknote.desktop.ui.tree.*
import com.darknote.desktop.ui.dialogs.RenameDialog
import com.darknote.desktop.ui.screens.SettingsScreen
import com.darknote.desktop.viewmodel.SnippetTreeViewModel
import com.darknote.desktop.viewmodel.AuthState
import com.darknote.desktop.viewmodel.AuthViewModel
import com.darknote.sync.client.DropboxClientFactory
import com.darknote.sync.engine.SyncEngine
import com.darknote.sync.engine.SyncState
import com.darknote.sync.watcher.FileWatcher
import com.darknote.sync.watcher.WatcherSync
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// Save status enum
sealed class SaveStatus {
    object Idle : SaveStatus()
    object Saving : SaveStatus()
    object Saved : SaveStatus()
    object Error : SaveStatus()
}

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    
    // Initialize database and repositories
    val databaseFactory = remember {
        com.darknote.persistence.database.DatabaseFactory(
            com.darknote.persistence.database.JvmDriverFactory()
        )
    }
    
    // Storage service
    val storageService = remember {
        FileStorageService(
            File(System.getProperty("user.home"), ".config/darknote")
        )
    }
    
    val viewModel = remember {
        SnippetTreeViewModel(
            snippetRepository = databaseFactory.snippetRepository,
            folderRepository = databaseFactory.folderRepository,
            fileStorageService = storageService,
            scope = scope
        )
    }
    
    val clipboardManager = remember {
        DesktopClipboardManager(ClipboardSanitizer(ClipboardSettings.DEFAULT))
    }
    
    // Dropbox client
    val dropboxClient = remember { DropboxClientFactory.create() }
    
    // Sync Engine
    val syncEngine = remember {
        SyncEngine(
            snippetRepository = databaseFactory.snippetRepository,
            dropboxClient = dropboxClient,
            coroutineScope = scope
        )
    }
    
    // Auth ViewModel
    val authViewModel = remember {
        AuthViewModel(dropboxClient)
    }
    
    // File watcher for auto-sync
    val watcherSync = remember {
        WatcherSync(
            syncEngine = syncEngine,
            scope = scope,
            snippetsDir = File(System.getProperty("user.home"), ".config/darknote/snippets")
        )
    }
    val isFileWatching by watcherSync.isWatching.collectAsState()
    
    // Start/stop file watcher based on auth state
    val authState by authViewModel.authState
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                if (!watcherSync.isRunning) {
                    watcherSync.start()
                }
            }
            is AuthState.NotAuthenticated, is AuthState.Offline -> {
                // Keep watching for local changes even when not authenticated
                // so we can queue syncs for later
            }
            else -> {}
        }
    }
    
    // Start file watcher on app startup & stop on dispose
    DisposableEffect(Unit) {
        watcherSync.start()
        onDispose {
            watcherSync.stop()
        }
    }
    
    // Sync state
    val syncState by syncEngine.syncState.collectAsState()
    
    // Periodic retry when Offline or in retryable Error state
    LaunchedEffect(syncState) {
        while (true) {
            if (syncState is SyncState.Offline || (syncState is SyncState.Error && (syncState as SyncState.Error).retryable)) {
                delay(30_000L) // Retry every 30 seconds
                if (dropboxClient.isAuthorized()) {
                    println("[Main] Periodic retry: attempting sync after offline/error...")
                    syncEngine.sync()
                }
            } else {
                break // Stop looping when state is not Offline/Error
            }
        }
    }
    // Initialize demo data on first launch
    LaunchedEffect(Unit) {
        val initializer = DemoDataInitializer(
            folderRepository = databaseFactory.folderRepository,
            snippetRepository = databaseFactory.snippetRepository,
            fileStorageService = storageService
        )
        initializer.initializeIfEmpty()
    }
    
    val selectedItemId by viewModel.selectedItemId
    val visibleItems = viewModel.visibleItems
    
    // Editor state
    var editorContent by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var isModified by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<SaveStatus>(SaveStatus.Idle as SaveStatus) }
    
    // Dialog states
    var showRenameDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<TreeItem?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    
    // Focus tracking for keyboard shortcut guards
    var isEditorFocused by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    
    // Selected snippet
    val selectedSnippet = remember(selectedItemId) {
        viewModel.getSelectedSnippet()
    }
    
    // Load content when selection changes
    LaunchedEffect(selectedItemId) {
        selectedSnippet?.let { snippet ->
            val result = storageService.loadSnippetContent(snippet.localPath)
            val content = result.getOrDefault(snippet.content)
            editorContent = content
            originalContent = content
            isModified = false
            saveStatus = SaveStatus.Idle
        } ?: run {
            editorContent = ""
            originalContent = ""
            isModified = false
        }
    }
    
    // Auto-save after delay
    LaunchedEffect(editorContent) {
        if (isModified && editorContent != originalContent) {
            delay(2000) // 2 seconds auto-save
            selectedSnippet?.let { snippet ->
                saveStatus = SaveStatus.Saving
                scope.launch {
                    val updatedSnippet = snippet.copy(content = editorContent)
                    val result = storageService.saveSnippetContent(updatedSnippet)
                    if (result.isSuccess) {
                        saveStatus = SaveStatus.Saved
                        originalContent = editorContent
                        isModified = false
                        viewModel.updateSnippetContent(snippet.id, editorContent)
                    } else {
                        saveStatus = SaveStatus.Error
                    }
                }
            }
        }
    }
    
    // Save action (reused by Ctrl+S and Save button)
    fun performSave() {
        selectedSnippet?.let { snippet ->
            saveStatus = SaveStatus.Saving
            scope.launch {
                val updatedSnippet = snippet.copy(content = editorContent)
                val result = storageService.saveSnippetContent(updatedSnippet)
                if (result.isSuccess) {
                    saveStatus = SaveStatus.Saved
                    originalContent = editorContent
                    isModified = false
                    viewModel.updateSnippetContent(snippet.id, editorContent)
                    if (dropboxClient.isAuthorized()) {
                        syncEngine.sync()
                    }
                } else {
                    saveStatus = SaveStatus.Error
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { keyEvent ->
            // Only handle key down events
            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

            val isCtrl = keyEvent.isCtrlPressed
            val isShift = keyEvent.isShiftPressed

            when {
                // Ctrl+S - Save
                isCtrl && !isShift && keyEvent.key == Key.S -> {
                    // Don't intercept if search bar is focused (allow normal typing)
                    if (!isSearchFocused) {
                        performSave()
                        true
                    } else false
                }

                // Ctrl+N - New snippet
                isCtrl && !isShift && keyEvent.key == Key.N -> {
                    viewModel.createSnippet()
                    true
                }

                // Ctrl+Shift+N - New folder
                isCtrl && isShift && keyEvent.key == Key.N -> {
                    viewModel.createFolder()
                    true
                }

                // Ctrl+F - Focus search bar
                isCtrl && !isShift && keyEvent.key == Key.F -> {
                    searchFocusRequester.requestFocus()
                    true
                }

                // Delete - Delete selected item
                !isCtrl && keyEvent.key == Key.Delete -> {
                    // Don't fire if editor has focus (editor needs Delete key)
                    if (!isEditorFocused && selectedItemId != null) {
                        val itemToDelete = visibleItems.find {
                            it.item.id == selectedItemId
                        }?.item
                        when (itemToDelete) {
                            is TreeItem.FolderItem -> viewModel.deleteFolder(itemToDelete.id)
                            is TreeItem.SnippetItem -> viewModel.deleteSnippet(itemToDelete.id)
                            else -> {}
                        }
                        true
                    } else false
                }

                // F2 - Rename selected item
                !isCtrl && keyEvent.key == Key.F2 -> {
                    if (!isEditorFocused && selectedItemId != null) {
                        val itemToRenameShortcut = visibleItems.find {
                            it.item.id == selectedItemId
                        }?.item
                        if (itemToRenameShortcut != null) {
                            itemToRename = itemToRenameShortcut
                            showRenameDialog = true
                        }
                        true
                    } else false
                }

                else -> false
            }
        },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (showSettings) "DarkNote - Settings" else "DarkNote")
                        if (!showSettings && isModified) {
                            Text(
                                " ●",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                },
                actions = {
                    // Settings/Home button
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(
                            if (showSettings) Icons.Default.Home else Icons.Default.Settings,
                            if (showSettings) "Back to Main" else "Settings"
                        )
                    }
                    
                    
                    if (!showSettings) {
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Save status indicator
                        when (saveStatus) {
                            is SaveStatus.Saving -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            is SaveStatus.Saved -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Saved",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            is SaveStatus.Error -> Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            else -> {}
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Save button
                        IconButton(
                        onClick = { performSave() },
                        enabled = isModified
                    ) {
                        Icon(Icons.Default.Save, "Save")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // File watcher indicator
                    if (isFileWatching) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Watching for changes",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Sync status indicator
                    when (syncState) {
                        is SyncState.Syncing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    "Syncing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        is SyncState.Offline -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Offline",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Offline Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        is SyncState.AuthRequired -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Auth Required",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Auth Required - Reconnect",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        is SyncState.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Sync error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                if ((syncState as SyncState.Error).retryable) {
                                    Text(
                                        "Sync Error (retrying...)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        else -> {
                            // Show cloud icon if authenticated
                            if (dropboxClient.isAuthorized()) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Synced",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    }
                },
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
            // Sidebar - Tree View
            TreeView(
                state = TreeViewState(
                    items = visibleItems,
                    selectedItemId = selectedItemId,
                    searchQuery = viewModel.searchQuery.value
                ),
                onItemClick = { item -> viewModel.selectItem(item.id) },
                onItemToggle = { folder -> viewModel.toggleFolder(folder.id) },
                onCreateSnippet = { viewModel.createSnippet() },
                onCreateFolder = { viewModel.createFolder() },
                onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
                onRenameItem = { item ->
                    itemToRename = item
                    showRenameDialog = true
                },
                onDeleteItem = { item ->
                    when (item) {
                        is TreeItem.FolderItem -> viewModel.deleteFolder(item.id)
                        is TreeItem.SnippetItem -> viewModel.deleteSnippet(item.id)
                    }
                },
                searchFocusRequester = searchFocusRequester,
                onSearchFocusChanged = { focused -> isSearchFocused = focused },
                modifier = Modifier.width(280.dp)
            )
            
            HorizontalDivider(
                modifier = Modifier.fillMaxHeight().width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Editor Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                if (selectedSnippet != null) {
                    // Title
                    Text(
                        text = selectedSnippet.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Editor
                    OutlinedTextField(
                        value = editorContent,
                        onValueChange = { 
                            editorContent = it
                            isModified = it != originalContent
                            saveStatus = SaveStatus.Idle
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                isEditorFocused = focusState.isFocused
                            },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.copy(editorContent, sanitize = true)
                            }
                        ) {
                            Text("Copy Sanitized")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                clipboardManager.copy(editorContent, sanitize = false)
                            }
                        ) {
                            Text("Copy Raw")
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Status text
                        Text(
                            text = when (saveStatus) {
                                is SaveStatus.Saving -> "Saving..."
                                is SaveStatus.Saved -> "Saved"
                                is SaveStatus.Error -> "Error saving"
                                else -> if (isModified) "Modified" else ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (saveStatus) {
                                is SaveStatus.Saved -> MaterialTheme.colorScheme.primary
                                is SaveStatus.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                } else {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Select a snippet from the sidebar",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Or create a new one using + button",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
        
        // Rename dialog
        if (showRenameDialog && itemToRename != null) {
            RenameDialog(
                title = when (itemToRename) {
                    is TreeItem.FolderItem -> "Rename Folder"
                    is TreeItem.SnippetItem -> "Rename Snippet"
                    else -> "Rename"
                },
                currentName = itemToRename!!.name,
                onDismiss = {
                    showRenameDialog = false
                    itemToRename = null
                },
                onConfirm = { newName ->
                    when (val item = itemToRename) {
                        is TreeItem.FolderItem -> viewModel.renameFolder(item.id, newName)
                        is TreeItem.SnippetItem -> viewModel.renameSnippet(item.id, newName)
                        null -> {} // Should not happen
                    }
                    showRenameDialog = false
                    itemToRename = null
                }
            )
        }
        
        // Settings screen - full replacement
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                SettingsScreen(
                    authViewModel = authViewModel,
                    onClose = { showSettings = false },
                    modifier = Modifier.fillMaxSize()
                )
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
