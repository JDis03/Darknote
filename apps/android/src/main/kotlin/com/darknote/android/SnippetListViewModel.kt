package com.darknote.android

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darknote.core.clipboard.ClipboardManager
import com.darknote.core.model.Folder
import com.darknote.core.model.Snippet
import com.darknote.core.model.SyncStatus
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.storage.FileStorageService
import com.darknote.sync.engine.SyncEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
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

sealed class UiState {
    data object Loading : UiState()
    data object Success : UiState()
    data class Error(val message: String) : UiState()
}

@HiltViewModel
class SnippetListViewModel @Inject constructor(
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val storageService: FileStorageService,
    private val clipboardManager: ClipboardManager,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _allSnippets = MutableStateFlow<List<Snippet>>(emptyList())
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _copiedSnippetId = MutableStateFlow<String?>(null)
    val copiedSnippetId: StateFlow<String?> = _copiedSnippetId.asStateFlow()

    private val _snackbarData = MutableStateFlow<SnackbarData?>(null)
    val snackbarData: StateFlow<SnackbarData?> = _snackbarData.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _createState = MutableStateFlow<CreateSnippetState>(CreateSnippetState.Idle)
    val createState: StateFlow<CreateSnippetState> = _createState.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    val syncState: StateFlow<com.darknote.sync.engine.SyncState> = syncEngine.state

    private val recentSnippets: StateFlow<List<Snippet>> = _allSnippets
        .combine(_selectedFolderId) { snippets, _ ->
            snippets.sortedByDescending { it.modifiedAt }.take(5)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val deletedSnippets = mutableMapOf<String, Snippet>()
    private val deletionTimestamps = mutableMapOf<String, Long>()
    private val DELETION_RETENTION_MS = 5 * 60 * 1000L // 5 minutes

    init {
        loadSnippets()
        loadFolders()
        initialSync()
    }

    private fun initialSync() {
        viewModelScope.launch {
            try {
                Log.d("SnippetListViewModel", "Running initial sync...")
                syncEngine.sync()
                Log.d("SnippetListViewModel", "Initial sync completed")
                // Refresh UI after pulling remote data
                loadSnippets()
                loadFolders()
            } catch (e: Exception) {
                Log.w("SnippetListViewModel", "Initial sync failed: ${e.message}")
            }
        }
    }

    private fun loadSnippets() {
        viewModelScope.launch {
            snippetRepository.getAll()
                .catch { emit(emptyList()) }
                .collect { snippets ->
                    _allSnippets.value = snippets
                }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            folderRepository.getAll()
                .catch { emit(emptyList()) }
                .collect { folders ->
                    _folders.value = folders
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank() && query !in _recentSearches.value) {
            _recentSearches.value = (listOf(query) + _recentSearches.value).take(10)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun removeRecentSearch(query: String) {
        _recentSearches.value = _recentSearches.value.filter { it != query }
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

    fun copySnippet(snippet: Snippet) {
        viewModelScope.launch {
            val content = storageService.loadSnippetContent(snippet.localPath).getOrNull()
                ?: snippet.content

            clipboardManager.copy(content, sanitize = true)
            _copiedSnippetId.value = snippet.id
            showSnackbar(SnackbarData("Copied to clipboard"))

            kotlinx.coroutines.delay(2000)
            _copiedSnippetId.value = null
        }
    }

    fun copyRawSnippet(snippet: Snippet) {
        viewModelScope.launch {
            val content = storageService.loadSnippetContent(snippet.localPath).getOrNull()
                ?: snippet.content

            clipboardManager.copy(content, sanitize = false)
            _copiedSnippetId.value = snippet.id
            showSnackbar(SnackbarData("Copied raw content"))

            kotlinx.coroutines.delay(2000)
            _copiedSnippetId.value = null
        }
    }

    fun toggleFavorite(snippet: Snippet) {
        viewModelScope.launch {
            val updated = snippet.copy(isFavorite = !snippet.isFavorite)
            snippetRepository.update(updated)
            showSnackbar(
                SnackbarData(
                    if (updated.isFavorite) "Added to favorites" else "Removed from favorites"
                )
            )
        }
    }

    fun deleteSnippet(snippet: Snippet) {
        viewModelScope.launch {
            deletedSnippets[snippet.id] = snippet
            deletionTimestamps[snippet.id] = System.currentTimeMillis()
            snippetRepository.delete(snippet.id)
            
            // Trigger sync after delete
            triggerSync()
            
            // Schedule cleanup after retention period
            cleanupOldDeletions()
            
            showSnackbar(
                SnackbarData(
                    message = "\"${snippet.title}\" deleted",
                    actionLabel = "Undo"
                ) {
                    viewModelScope.launch {
                        restoreSnippet(snippet.id)
                    }
                }
            )
        }
    }
    
    private fun cleanupOldDeletions() {
        val cutoff = System.currentTimeMillis() - DELETION_RETENTION_MS
        val iterator = deletionTimestamps.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < cutoff) {
                deletedSnippets.remove(entry.key)
                iterator.remove()
            }
        }
    }

    private suspend fun restoreSnippet(snippetId: String) {
        val snippet = deletedSnippets.remove(snippetId) ?: return
        deletionTimestamps.remove(snippetId)
        snippetRepository.create(snippet)
        
        // Trigger sync after restore
        triggerSync()
        
        showSnackbar(SnackbarData("Snippet restored"))
    }

    fun createSnippet(title: String, content: String, language: String?, tags: List<String>, folderId: String?) {
        viewModelScope.launch {
            _createState.value = CreateSnippetState.Creating
            try {
                val id = UUID.randomUUID().toString()
                val localPath = storageService.generateSafePath(title)
                val snippet = Snippet(
                    id = id,
                    title = title,
                    content = content,
                    folderId = folderId,
                    tags = tags,
                    language = language,
                    isFavorite = false,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis(),
                    localPath = localPath,
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
                // Create in database
                val result = snippetRepository.create(snippet)
                if (result.isFailure) {
                    _createState.value = CreateSnippetState.Error(result.exceptionOrNull()?.message ?: "Failed to create snippet")
                    showSnackbar(SnackbarData("Failed to create snippet"))
                    return@launch
                }
                
                // Save to storage
                val storageResult = storageService.saveSnippetContent(snippet)
                if (storageResult.isFailure) {
                    _createState.value = CreateSnippetState.Error(storageResult.exceptionOrNull()?.message ?: "Failed to save snippet content")
                    showSnackbar(SnackbarData("Failed to save snippet content"))
                    return@launch
                }
                
                // Refresh the list to show new snippet
                loadSnippets()
                _createState.value = CreateSnippetState.Created
                
                // Trigger sync after create
                triggerSync()
                
                showSnackbar(SnackbarData("Snippet created"))
            } catch (e: Exception) {
                _createState.value = CreateSnippetState.Error(e.message ?: "Failed to create snippet")
                showSnackbar(SnackbarData("Failed to create snippet"))
            }
        }
    }

    fun renameSnippet(id: String, newTitle: String) {
        viewModelScope.launch {
            try {
                val snippet = _allSnippets.value.find { it.id == id } ?: return@launch
                val updated = snippet.copy(
                    title = newTitle,
                    modifiedAt = System.currentTimeMillis()
                )
                val result = snippetRepository.update(updated)
                if (result.isFailure) {
                    showSnackbar(SnackbarData("Failed to rename snippet: ${result.exceptionOrNull()?.message}"))
                    return@launch
                }
                
                loadSnippets()
                triggerSync()
                showSnackbar(SnackbarData("Snippet renamed to \"$newTitle\""))
            } catch (e: Exception) {
                Log.e("SnippetListViewModel", "Failed to rename snippet", e)
                showSnackbar(SnackbarData("Failed to rename snippet: ${e.message}"))
            }
        }
    }

    fun updateSnippet(snippet: Snippet) {
        viewModelScope.launch {
            try {
                Log.d("SnippetListViewModel", "Updating snippet: ${snippet.title}")
                val updated = snippet.copy(modifiedAt = System.currentTimeMillis())
                
                // Update in database
                val result = snippetRepository.update(updated)
                if (result.isFailure) {
                    Log.e("SnippetListViewModel", "Database update failed: ${result.exceptionOrNull()?.message}")
                    showSnackbar(SnackbarData("Failed to save snippet"))
                    return@launch
                }
                Log.d("SnippetListViewModel", "Database update successful")
                
                // Save to storage
                val storageResult = storageService.saveSnippetContent(updated)
                if (storageResult.isFailure) {
                    Log.e("SnippetListViewModel", "Storage update failed: ${storageResult.exceptionOrNull()?.message}")
                    showSnackbar(SnackbarData("Failed to save snippet content"))
                    return@launch
                }
                Log.d("SnippetListViewModel", "Storage update successful")
                
                // Refresh the list to show updated content
                loadSnippets()
                Log.d("SnippetListViewModel", "Snippet list refreshed")
                
                // Trigger sync after update
                triggerSync()
                
                showSnackbar(SnackbarData("Snippet saved"))
            } catch (e: Exception) {
                Log.e("SnippetListViewModel", "Failed to update snippet", e)
                showSnackbar(SnackbarData("Failed to save snippet: ${e.message}"))
            }
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            loadSnippets()
            loadFolders()
            _isRefreshing.value = false
        }
    }

    fun showSnackbar(data: SnackbarData) {
        _snackbarData.value = data
    }

    fun clearSnackbar() {
        _snackbarData.value = null
    }
    
    suspend fun loadSnippetWithContent(snippet: Snippet): Snippet {
        return try {
            val contentResult = storageService.loadSnippetContent(snippet.localPath)
            if (contentResult.isSuccess) {
                snippet.copy(content = contentResult.getOrDefault(""))
            } else {
                Log.w("SnippetListViewModel", "Failed to load content for ${snippet.title}: ${contentResult.exceptionOrNull()?.message}")
                snippet.copy(content = "")
            }
        } catch (e: Exception) {
            Log.e("SnippetListViewModel", "Error loading snippet content", e)
            snippet.copy(content = "")
        }
    }

    fun shareSnippet(snippet: Snippet, context: android.content.Context) {
        viewModelScope.launch {
            val content = storageService.loadSnippetContent(snippet.localPath).getOrNull()
                ?: snippet.content

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_SUBJECT, snippet.title)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share \"${snippet.title}\"")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        }
    }

    fun clearCreateState() {
        _createState.value = CreateSnippetState.Idle
    }

    fun syncNow() {
        viewModelScope.launch {
            try {
                Log.d("SnippetListViewModel", "Manual sync triggered")
                syncEngine.sync()
                Log.d("SnippetListViewModel", "Manual sync completed")
                loadSnippets()
                loadFolders()
                showSnackbar(SnackbarData("Sync complete"))
            } catch (e: Exception) {
                Log.w("SnippetListViewModel", "Manual sync failed: ${e.message}")
                showSnackbar(SnackbarData("Sync failed: ${e.message}"))
            }
        }
    }

    // FOLDER CRUD OPERATIONS

    fun createFolder(name: String, parentId: String?) {
        viewModelScope.launch {
            try {
                val id = UUID.randomUUID().toString()
                val folder = Folder(
                    id = id,
                    name = name,
                    parentId = parentId,
                    sortOrder = 0,
                    createdAt = System.currentTimeMillis()
                )
                val result = folderRepository.create(folder)
                if (result.isFailure) {
                    showSnackbar(SnackbarData("Failed to create folder: ${result.exceptionOrNull()?.message}"))
                    return@launch
                }
                
                // Trigger sync after create
                triggerSync()
                showSnackbar(SnackbarData("Folder \"$name\" created"))
            } catch (e: Exception) {
                Log.e("SnippetListViewModel", "Failed to create folder", e)
                showSnackbar(SnackbarData("Failed to create folder: ${e.message}"))
            }
        }
    }

    fun renameFolder(id: String, newName: String) {
        viewModelScope.launch {
            try {
                val folder = _folders.value.find { it.id == id } ?: return@launch
                val updated = folder.copy(name = newName)
                val result = folderRepository.update(updated)
                if (result.isFailure) {
                    showSnackbar(SnackbarData("Failed to rename folder: ${result.exceptionOrNull()?.message}"))
                    return@launch
                }
                
                // Trigger sync after update
                triggerSync()
                showSnackbar(SnackbarData("Folder renamed to \"$newName\""))
            } catch (e: Exception) {
                Log.e("SnippetListViewModel", "Failed to rename folder", e)
                showSnackbar(SnackbarData("Failed to rename folder: ${e.message}"))
            }
        }
    }

    fun deleteFolder(id: String, moveChildrenToParent: Boolean) {
        viewModelScope.launch {
            try {
                val folder = _folders.value.find { it.id == id } ?: return@launch
                val result = folderRepository.delete(id, moveChildrenToParent)
                if (result.isFailure) {
                    showSnackbar(SnackbarData("Failed to delete folder: ${result.exceptionOrNull()?.message}"))
                    return@launch
                }
                
                // Clear selection if deleted folder was selected
                if (_selectedFolderId.value == id) {
                    _selectedFolderId.value = null
                }
                
                // Trigger sync after delete
                triggerSync()
                showSnackbar(SnackbarData("Folder \"${folder.name}\" deleted"))
            } catch (e: Exception) {
                Log.e("SnippetListViewModel", "Failed to delete folder", e)
                showSnackbar(SnackbarData("Failed to delete folder: ${e.message}"))
            }
        }
    }

    /**
     * Triggers sync in the background
     * Only syncs if the sync engine is in a valid state for syncing
     */
    private fun triggerSync() {
        viewModelScope.launch {
            try {
                Log.d("SnippetListViewModel", "Triggering sync...")
                syncEngine.sync()
                Log.d("SnippetListViewModel", "Sync completed successfully")
            } catch (e: Exception) {
                Log.w("SnippetListViewModel", "Sync failed: ${e.message}")
            }
        }
    }
}