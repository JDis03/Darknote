package com.darknote.desktop.viewmodel

import com.darknote.core.clipboard.ClipboardManager
import com.darknote.core.model.Folder
import com.darknote.core.model.Snippet
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.storage.FileStorageService
import com.darknote.sync.engine.SyncEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class SnackbarData(
    val message: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null
)

enum class SortOrder {
    MODIFIED_DESC,
    CREATED_DESC,
    TITLE_ASC,
    MOST_USED
}

sealed class CreateSnippetState {
    data object Idle : CreateSnippetState()
    data object Creating : CreateSnippetState()
    data object Created : CreateSnippetState()
    data class Error(val message: String) : CreateSnippetState()
}

class SnippetListViewModel(
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val storageService: FileStorageService,
    private val clipboardManager: ClipboardManager,
    private val syncEngine: SyncEngine
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _allSnippets = snippetRepository.getAll()
        .catch { emit(emptyList()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    private val _showFavoritesOnly = MutableStateFlow(false)
    private val _sortOrder = MutableStateFlow(SortOrder.MODIFIED_DESC)
    private val _selectedFolderId = MutableStateFlow<String?>(null)
    private val _selectedTag = MutableStateFlow<String?>(null)

    val filteredSnippets = combine(
        combine(_allSnippets, _searchQuery, _showFavoritesOnly) { snippets, query, favoritesOnly ->
            Triple(snippets, query, favoritesOnly)
        },
        combine(_sortOrder, _selectedFolderId, _selectedTag) { sortOrder, folderId, tag ->
            Triple(sortOrder, folderId, tag)
        }
    ) { (snippets, query, favoritesOnly), (sortOrder, folderId, tag) ->
        var filtered = snippets.filter { snippet ->
            val matchesQuery = query.isBlank() ||
                snippet.title.contains(query, ignoreCase = true) ||
                snippet.content.contains(query, ignoreCase = true) ||
                snippet.tags.any { it.contains(query, ignoreCase = true) } ||
                (snippet.language?.contains(query, ignoreCase = true) == true)

            val matchesFavorite = !favoritesOnly || snippet.isFavorite
            val matchesFolder = folderId == null || snippet.folderId == folderId
            val matchesTag = tag == null || snippet.tags.any { it.equals(tag, ignoreCase = true) }

            matchesQuery && matchesFavorite && matchesFolder && matchesTag
        }

        filtered = when (sortOrder) {
            SortOrder.MODIFIED_DESC -> filtered.sortedByDescending { it.modifiedAt }
            SortOrder.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
            SortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.MOST_USED -> filtered.sortedByDescending { it.isFavorite }
        }

        filtered
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _snackbarData = MutableStateFlow<SnackbarData?>(null)
    val snackbarData: StateFlow<SnackbarData?> = _snackbarData.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _createState = MutableStateFlow<CreateSnippetState>(CreateSnippetState.Idle)
    val createState: StateFlow<CreateSnippetState> = _createState.asStateFlow()

    val syncState = syncEngine.state
    val syncLogs = syncEngine.logs

    init {
        loadFolders()
        initialSync()
    }

    private fun loadFolders() {
        scope.launch {
            folderRepository.getAll()
                .catch { emit(emptyList()) }
                .collect { folders ->
                    _folders.value = folders
                }
        }
    }

    private fun initialSync() {
        scope.launch {
            delay(1000)
            triggerSync()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun toggleShowFavorites() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun createFolder(name: String, parentId: String? = null, callback: (String) -> Unit = {}) {
        scope.launch {
            val folderId = UUID.randomUUID().toString()
            val folder = Folder(
                id = folderId,
                name = name,
                parentId = parentId,
                sortOrder = 0,
                createdAt = System.currentTimeMillis()
            )
            val result = folderRepository.create(folder)
            if (result.isSuccess) {
                showSnackbar(SnackbarData("Folder created"))
                callback(folderId)
            } else {
                showSnackbar(SnackbarData("Failed to create folder"))
            }
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        scope.launch {
            folderRepository.getById(folderId)?.let { folder ->
                val updated = folder.copy(name = newName)
                val result = folderRepository.update(updated)
                if (result.isSuccess) {
                    showSnackbar(SnackbarData("Folder renamed"))
                } else {
                    showSnackbar(SnackbarData("Failed to rename folder"))
                }
            }
        }
    }

    fun deleteFolder(folderId: String) {
        scope.launch {
            val result = folderRepository.delete(folderId, moveChildrenToParent = true)
            if (result.isSuccess) {
                showSnackbar(SnackbarData("Folder deleted"))
                if (_selectedFolderId.value == folderId) {
                    _selectedFolderId.value = null
                }
            } else {
                showSnackbar(SnackbarData("Failed to delete folder"))
            }
        }
    }

    fun createSnippet(title: String = "", content: String = "", folderId: String? = null, callback: (String) -> Unit = {}) {
        scope.launch {
            _createState.value = CreateSnippetState.Creating
            
            val snippetId = UUID.randomUUID().toString()
            val snippet = Snippet(
                id = snippetId,
                title = title.ifBlank { "Untitled" },
                content = content,
                localPath = "snippets/${snippetId.replace("-", "_")}.txt",
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                folderId = folderId,
                tags = emptyList(),
                language = null,
                isFavorite = false,
                syncStatus = com.darknote.core.model.SyncStatus.NOT_SYNCED
            )
            
            val result = snippetRepository.create(snippet)
            if (result.isSuccess) {
                storageService.saveSnippetContent(snippet)
                _createState.value = CreateSnippetState.Created
                callback(snippetId)
                delay(500)
                _createState.value = CreateSnippetState.Idle
                triggerSync()
            } else {
                _createState.value = CreateSnippetState.Error("Failed to create snippet")
            }
        }
    }

    fun updateSnippet(snippet: Snippet) {
        scope.launch {
            val updated = snippet.copy(modifiedAt = System.currentTimeMillis())
            snippetRepository.update(updated)
            storageService.saveSnippetContent(updated)
            triggerSync()
        }
    }

    fun deleteSnippet(snippet: Snippet) {
        scope.launch {
            storageService.deleteSnippetFile(snippet.localPath)
            snippetRepository.delete(snippet.id)
            showSnackbar(SnackbarData("Snippet deleted"))
            triggerSync()
        }
    }

    fun toggleFavorite(snippet: Snippet) {
        scope.launch {
            val updated = snippet.copy(isFavorite = !snippet.isFavorite)
            snippetRepository.update(updated)
        }
    }

    fun copyToClipboard(snippet: Snippet) {
        scope.launch {
            try {
                clipboardManager.copy(snippet.content, sanitize = false)
                showSnackbar(SnackbarData("Copied to clipboard"))
            } catch (e: Exception) {
                showSnackbar(SnackbarData("Failed to copy"))
            }
        }
    }

    fun loadSnippetWithContent(snippet: Snippet): Snippet {
        return runBlocking {
            val contentResult = storageService.loadSnippetContent(snippet.localPath)
            snippet.copy(content = contentResult.getOrNull() ?: snippet.content)
        }
    }

    fun triggerSync() {
        scope.launch {
            syncEngine.sync()
        }
    }

    private fun showSnackbar(data: SnackbarData) {
        _snackbarData.value = data
        scope.launch {
            delay(3000)
            _snackbarData.value = null
        }
    }

    fun onDispose() {
        scope.cancel()
    }
}
