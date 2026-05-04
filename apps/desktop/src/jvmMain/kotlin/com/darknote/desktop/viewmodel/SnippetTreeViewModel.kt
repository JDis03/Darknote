package com.darknote.desktop.viewmodel

import com.darknote.core.model.Folder
import com.darknote.core.model.Snippet
import com.darknote.core.model.SyncStatus
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.storage.FileStorageService
import com.darknote.desktop.ui.tree.TreeItem
import com.darknote.desktop.ui.tree.TreeItemNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.runtime.State

class SnippetTreeViewModel(
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val fileStorageService: FileStorageService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _snippets = MutableStateFlow<List<Snippet>>(emptyList())
    val snippets: StateFlow<List<Snippet>> = _snippets.asStateFlow()

    init {
        folderRepository.getAll()
            .onEach { _folders.value = it }
            .launchIn(scope)

        snippetRepository.getAll()
            .onEach { _snippets.value = it }
            .launchIn(scope)
    }

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId: StateFlow<String?> = _selectedItemId.asStateFlow()

    private val _expandedFolderIds = MutableStateFlow<Set<String>>(setOf("1"))
    val expandedFolderIds: StateFlow<Set<String>> = _expandedFolderIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val visibleItems: StateFlow<List<TreeItemNode>> = combine(
        _folders, _snippets, _expandedFolderIds, _searchQuery
    ) { folders, snippets, expandedIds, query ->
        val nodes = mutableListOf<TreeItemNode>()
        val q = query.lowercase()

        if (q.isNotEmpty()) {
            snippets.filter {
                it.title.lowercase().contains(q) ||
                    it.tags.any { tag -> tag.lowercase().contains(q) }
            }.forEach { snippet ->
                nodes.add(TreeItemNode(
                    item = TreeItem.SnippetItem(
                        id = snippet.id, name = snippet.title,
                        language = snippet.language, isFavorite = snippet.isFavorite
                    ), depth = 0, isVisible = true
                ))
            }
            folders.filter { it.name.lowercase().contains(q) }.forEach { folder ->
                val snippetCount = snippets.count { it.folderId == folder.id }
                nodes.add(TreeItemNode(
                    item = TreeItem.FolderItem(
                        id = folder.id, name = folder.name,
                        isExpanded = false, childCount = snippetCount
                    ), depth = 0, isVisible = true
                ))
            }
            return@combine nodes
        }

        folders.forEach { folder ->
            val snippetCount = snippets.count { it.folderId == folder.id }
            val isExpanded = expandedIds.contains(folder.id)
            nodes.add(TreeItemNode(
                item = TreeItem.FolderItem(
                    id = folder.id, name = folder.name,
                    isExpanded = isExpanded, childCount = snippetCount
                ), depth = 0, isVisible = true
            ))
            if (isExpanded) {
                snippets.filter { it.folderId == folder.id }.forEach { snippet ->
                    nodes.add(TreeItemNode(
                        item = TreeItem.SnippetItem(
                            id = snippet.id, name = snippet.title,
                            language = snippet.language, isFavorite = snippet.isFavorite
                        ), depth = 1, isVisible = true
                    ))
                }
            }
        }

        snippets.filter { it.folderId == null }.forEach { snippet ->
            nodes.add(TreeItemNode(
                item = TreeItem.SnippetItem(
                    id = snippet.id, name = snippet.title,
                    language = snippet.language, isFavorite = snippet.isFavorite
                ), depth = 0, isVisible = true
            ))
        }

        nodes
    }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    fun selectItem(itemId: String) {
        _selectedItemId.value = itemId
    }

    fun toggleFolder(folderId: String) {
        val current = _expandedFolderIds.value.toMutableSet()
        if (current.contains(folderId)) current.remove(folderId) else current.add(folderId)
        _expandedFolderIds.value = current
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createSnippet() {
        val targetFolderId = when {
            _selectedItemId.value?.let { id -> _folders.value.any { it.id == id } } == true
                -> _selectedItemId.value
            _selectedItemId.value?.let { id -> _snippets.value.find { it.id == id }?.folderId } != null
                -> _snippets.value.find { it.id == _selectedItemId.value }?.folderId
            else -> null
        }

        val folderName = targetFolderId?.let { fid -> _folders.value.find { it.id == fid }?.name } ?: "Root"

        val newSnippet = Snippet(
            id = UUID.randomUUID().toString(),
            title = "New Snippet",
            content = "# Add your code here\n# Created in: $folderName",
            folderId = targetFolderId,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            localPath = fileStorageService.generateSafePath("New Snippet"),
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        scope.launch {
            snippetRepository.create(newSnippet)
            fileStorageService.saveSnippetContent(newSnippet)
        }

        targetFolderId?.let { fid ->
            val currentExpanded = _expandedFolderIds.value.toMutableSet()
            currentExpanded.add(fid)
            _expandedFolderIds.value = currentExpanded
        }

        _selectedItemId.value = newSnippet.id
        println("Created snippet: ${newSnippet.title} in folder: $folderName")
    }

    fun createFolder(parentId: String? = null) {
        val newFolder = Folder(
            id = UUID.randomUUID().toString(),
            name = "New Folder",
            parentId = parentId,
            sortOrder = _folders.value.size,
            createdAt = System.currentTimeMillis()
        )

        scope.launch { folderRepository.create(newFolder) }

        val currentExpanded = _expandedFolderIds.value.toMutableSet()
        currentExpanded.add(newFolder.id)
        _expandedFolderIds.value = currentExpanded

        println("Created folder: ${newFolder.name}")
    }

    fun getSelectedSnippet(): Snippet? {
        val selectedId = _selectedItemId.value ?: return null
        return _snippets.value.find { it.id == selectedId }
    }

    fun updateSnippetContent(id: String, newContent: String) {
        scope.launch {
            val snippet = snippetRepository.getById(id) ?: return@launch
            val updated = snippet.copy(
                content = newContent,
                modifiedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            snippetRepository.update(updated)
            println("Snippet updated: ${snippet.title}")
        }
    }

    fun deleteSnippet(id: String) {
        scope.launch {
            val snippet = snippetRepository.getById(id) ?: return@launch
            fileStorageService.deleteSnippetFile(snippet.localPath)
            snippetRepository.delete(id)
            println("Snippet deleted: ${snippet.title}")
        }
    }

    fun deleteFolder(id: String) {
        scope.launch {
            folderRepository.delete(id, moveChildrenToParent = true)
            println("Folder deleted: $id")
        }
    }

    fun renameSnippet(id: String, newTitle: String) {
        scope.launch {
            val snippet = snippetRepository.getById(id) ?: return@launch
            snippetRepository.update(snippet.copy(title = newTitle, modifiedAt = System.currentTimeMillis()))
            println("Snippet renamed to: $newTitle")
        }
    }

    fun renameFolder(id: String, newName: String) {
        scope.launch {
            val folder = folderRepository.getById(id) ?: return@launch
            folderRepository.update(folder.copy(name = newName))
            println("Folder renamed to: $newName")
        }
    }
}
