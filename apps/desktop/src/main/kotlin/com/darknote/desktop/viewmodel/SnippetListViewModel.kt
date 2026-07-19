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

/** Timing constants (millis) — kept here so they are easy to tune & audit. */
private object Timing {
    const val FLOW_STOP_TIMEOUT_MS = 5_000L
    const val INITIAL_SYNC_DELAY_MS = 1_000L
    const val POST_CREATE_DELAY_MS = 500L
    const val SNACKBAR_DURATION_MS = 3_000L
}

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

/** Single source of truth for snippet list filtering. */
private data class FilterInputs(
    val snippets: List<Snippet> = emptyList(),
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val folderId: String? = null,
    val tag: String? = null,
)

class SnippetListViewModel(
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val storageService: FileStorageService,
    private val clipboardManager: ClipboardManager,
    private val syncEngine: SyncEngine
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Reactive folder list (single source of truth, no manual collect)
    val folders: StateFlow<List<Folder>> = folderRepository.getAll()
        .catch { emit(emptyList()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(Timing.FLOW_STOP_TIMEOUT_MS), emptyList())

    // Reactive snippet list
    private val allSnippets: StateFlow<List<Snippet>> = snippetRepository.getAll()
        .catch { emit(emptyList()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(Timing.FLOW_STOP_TIMEOUT_MS), emptyList())

    // Single combined input flow for filtering
    private val filterInputs = MutableStateFlow(FilterInputs(snippets = emptyList()))

    init {
        // Seed filter inputs from the snippet repository
        scope.launch {
            allSnippets.collect { filterInputs.value = filterInputs.value.copy(snippets = it) }
        }
        scope.launch {
            delay(Timing.INITIAL_SYNC_DELAY_MS)
            syncEngine.sync()
        }
    }

    val filteredSnippets: StateFlow<List<Snippet>> = filterInputs
        .map { applyFilters(it) }
        .stateIn(scope, SharingStarted.WhileSubscribed(Timing.FLOW_STOP_TIMEOUT_MS), emptyList())

    val searchQuery: StateFlow<String> = filterInputs
        .map { it.query }
        .stateIn(scope, SharingStarted.Eagerly, "")

    val showFavoritesOnly: StateFlow<Boolean> = filterInputs
        .map { it.favoritesOnly }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val sortOrder: StateFlow<SortOrder> = filterInputs
        .map { it.sortOrder }
        .stateIn(scope, SharingStarted.Eagerly, SortOrder.MODIFIED_DESC)

    val selectedFolderId: StateFlow<String?> = filterInputs
        .map { it.folderId }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val selectedTag: StateFlow<String?> = filterInputs
        .map { it.tag }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val _snackbarData = MutableStateFlow<SnackbarData?>(null)
    val snackbarData: StateFlow<SnackbarData?> = _snackbarData.asStateFlow()

    private val _createState = MutableStateFlow<CreateSnippetState>(CreateSnippetState.Idle)
    val createState: StateFlow<CreateSnippetState> = _createState.asStateFlow()

    val syncState = syncEngine.state
    val syncLogs = syncEngine.logs

    private fun applyFilters(input: FilterInputs): List<Snippet> {
        val filtered = input.snippets.filter { snippet ->
            val matchesQuery = input.query.isBlank() ||
                snippet.title.contains(input.query, ignoreCase = true) ||
                snippet.content.contains(input.query, ignoreCase = true) ||
                snippet.tags.any { it.contains(input.query, ignoreCase = true) } ||
                (snippet.language?.contains(input.query, ignoreCase = true) == true)

            val matchesFavorite = !input.favoritesOnly || snippet.isFavorite
            val matchesFolder = input.folderId == null || snippet.folderId == input.folderId
            val matchesTag = input.tag == null || snippet.tags.any { it.equals(input.tag, ignoreCase = true) }

            matchesQuery && matchesFavorite && matchesFolder && matchesTag
        }

        return when (input.sortOrder) {
            SortOrder.MODIFIED_DESC -> filtered.sortedByDescending { it.modifiedAt }
            SortOrder.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
            SortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.MOST_USED -> filtered.sortedByDescending { it.isFavorite }
        }
    }

    fun onSearchQueryChange(query: String) {
        filterInputs.value = filterInputs.value.copy(query = query)
    }

    fun clearSearch() {
        filterInputs.value = filterInputs.value.copy(query = "")
    }

    fun toggleShowFavorites() {
        filterInputs.value = filterInputs.value.copy(favoritesOnly = !filterInputs.value.favoritesOnly)
    }

    fun setSortOrder(order: SortOrder) {
        filterInputs.value = filterInputs.value.copy(sortOrder = order)
    }

    fun selectFolder(folderId: String?) {
        filterInputs.value = filterInputs.value.copy(folderId = folderId)
    }

    fun selectTag(tag: String?) {
        filterInputs.value = filterInputs.value.copy(tag = tag)
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
                if (filterInputs.value.folderId == folderId) {
                    filterInputs.value = filterInputs.value.copy(folderId = null)
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
                delay(Timing.POST_CREATE_DELAY_MS)
                _createState.value = CreateSnippetState.Idle
                syncEngine.sync()
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
            syncEngine.sync()
        }
    }

    fun deleteSnippet(snippet: Snippet) {
        scope.launch {
            storageService.deleteSnippetFile(snippet.localPath)
            snippetRepository.delete(snippet.id)
            showSnackbar(SnackbarData("Snippet deleted"))
            syncEngine.sync()
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
            runCatching { clipboardManager.copy(snippet.content, sanitize = false) }
                .onSuccess { showSnackbar(SnackbarData("Copied to clipboard")) }
                .onFailure { showSnackbar(SnackbarData("Failed to copy")) }
        }
    }

    /** Non-blocking variant — replaces previous `runBlocking` on UI thread. */
    suspend fun loadSnippetWithContent(snippet: Snippet): Snippet {
        val contentResult = storageService.loadSnippetContent(snippet.localPath)
        return snippet.copy(content = contentResult.getOrNull() ?: snippet.content)
    }

    fun triggerSync() {
        scope.launch { syncEngine.sync() }
    }

    private fun showSnackbar(data: SnackbarData) {
        _snackbarData.value = data
        scope.launch {
            delay(Timing.SNACKBAR_DURATION_MS)
            _snackbarData.value = null
        }
    }

    fun onDispose() {
        scope.cancel()
    }
}
