package com.darknote.desktop.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class SnippetTreeViewModel(
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val fileStorageService: FileStorageService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Repository data - Folders (mutable state for UI updates)
    private val _folders = mutableStateOf<List<Folder>>(emptyList())
    val folders: State<List<Folder>> = _folders
    
    // Repository data - Snippets (mutable state for UI updates)
    private val _snippets = mutableStateOf<List<Snippet>>(emptyList())
    val snippets: State<List<Snippet>> = _snippets
    
    init {
        // Observe folder changes
        folderRepository.getAll()
            .onEach { folders -> _folders.value = folders }
            .launchIn(scope)
        
        // Observe snippet changes
        snippetRepository.getAll()
            .onEach { snippets -> _snippets.value = snippets }
            .launchIn(scope)
    }
    
    private val _selectedItemId = mutableStateOf<String?>(null)
    val selectedItemId: State<String?> = _selectedItemId
    
    private val _expandedFolderIds = mutableStateOf<Set<String>>(setOf("1")) // Scripts folder expanded by default
    val expandedFolderIds: State<Set<String>> = _expandedFolderIds
    
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery
    
    // Computed property that updates when state changes
    val visibleItems: List<TreeItemNode>
        get() = computeVisibleItems()
    
    private fun computeVisibleItems(): List<TreeItemNode> {
        val nodes = mutableListOf<TreeItemNode>()
        val expandedIds = _expandedFolderIds.value
        val currentFolders = _folders.value
        val currentSnippets = _snippets.value
        val query = _searchQuery.value.lowercase()
        
        // If searching, only show matching items
        if (query.isNotEmpty()) {
            // Show matching snippets
            currentSnippets
                .filter { 
                    it.title.lowercase().contains(query) || 
                    it.tags.any { tag -> tag.lowercase().contains(query) }
                }
                .forEach { snippet ->
                    nodes.add(TreeItemNode(
                        item = TreeItem.SnippetItem(
                            id = snippet.id,
                            name = snippet.title,
                            language = snippet.language,
                            isFavorite = snippet.isFavorite
                        ),
                        depth = 0,
                        isVisible = true
                    ))
                }
            
            // Show matching folders
            currentFolders
                .filter { it.name.lowercase().contains(query) }
                .forEach { folder ->
                    val snippetCount = currentSnippets.count { it.folderId == folder.id }
                    nodes.add(TreeItemNode(
                        item = TreeItem.FolderItem(
                            id = folder.id,
                            name = folder.name,
                            isExpanded = false,
                            childCount = snippetCount
                        ),
                        depth = 0,
                        isVisible = true
                    ))
                }
            
            return nodes
        }
        
        // Add folders
        currentFolders.forEach { folder ->
            val snippetCount = currentSnippets.count { it.folderId == folder.id }
            val isExpanded = expandedIds.contains(folder.id)
            
            nodes.add(TreeItemNode(
                item = TreeItem.FolderItem(
                    id = folder.id,
                    name = folder.name,
                    isExpanded = isExpanded,
                    childCount = snippetCount
                ),
                depth = 0,
                isVisible = true
            ))
            
            // Show snippets if folder is expanded
            if (isExpanded) {
                currentSnippets.filter { it.folderId == folder.id }.forEach { snippet ->
                    nodes.add(TreeItemNode(
                        item = TreeItem.SnippetItem(
                            id = snippet.id,
                            name = snippet.title,
                            language = snippet.language,
                            isFavorite = snippet.isFavorite
                        ),
                        depth = 1,
                        isVisible = true
                    ))
                }
            }
        }
        
        // Root snippets (no folder)
        currentSnippets.filter { it.folderId == null }.forEach { snippet ->
            nodes.add(TreeItemNode(
                item = TreeItem.SnippetItem(
                    id = snippet.id,
                    name = snippet.title,
                    language = snippet.language,
                    isFavorite = snippet.isFavorite
                ),
                depth = 0,
                isVisible = true
            ))
        }
        
        return nodes
    }
    
    fun selectItem(itemId: String) {
        _selectedItemId.value = itemId
    }
    
    fun toggleFolder(folderId: String) {
        val current = _expandedFolderIds.value.toMutableSet()
        if (current.contains(folderId)) {
            current.remove(folderId)
        } else {
            current.add(folderId)
        }
        _expandedFolderIds.value = current
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Create new snippet in the selected folder or in root if no folder selected.
     */
    fun createSnippet() {
        // Determine target folder:
        // 1. If a folder is selected, use it
        // 2. If a snippet is selected, use its folder
        // 3. Otherwise, create in root (null)
        val targetFolderId = when {
            // Check if selected item is a folder
            _selectedItemId.value?.let { id ->
                _folders.value.any { it.id == id }
            } == true -> _selectedItemId.value
            
            // Check if selected item is a snippet, get its folder
            _selectedItemId.value?.let { id ->
                _snippets.value.find { it.id == id }?.folderId
            } != null -> _snippets.value.find { it.id == _selectedItemId.value }?.folderId
            
            // Default: root
            else -> null
        }
        
        val folderName = targetFolderId?.let { fid ->
            _folders.value.find { it.id == fid }?.name
        } ?: "Root"
        
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
        
        // Save to repository
        scope.launch {
            snippetRepository.create(newSnippet)
            fileStorageService.saveSnippetContent(newSnippet)
        }
        
        // Expand target folder if not root
        targetFolderId?.let { fid ->
            val currentExpanded = _expandedFolderIds.value.toMutableSet()
            currentExpanded.add(fid)
            _expandedFolderIds.value = currentExpanded
        }
        
        // Select the new snippet
        _selectedItemId.value = newSnippet.id
        println("Created snippet: ${newSnippet.title} in folder: $folderName")
    }
    
    /**
     * Create new folder.
     */
    fun createFolder(parentId: String? = null) {
        val newFolder = Folder(
            id = UUID.randomUUID().toString(),
            name = "New Folder",
            parentId = parentId,
            sortOrder = _folders.value.size,
            createdAt = System.currentTimeMillis()
        )
        
        // Save to repository
        scope.launch {
            folderRepository.create(newFolder)
        }
        
        // Expand the new folder
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
            val snippet = snippetRepository.getById(id)
            if (snippet != null) {
                val updated = snippet.copy(
                    content = newContent,
                    modifiedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
                snippetRepository.update(updated)
                println("Snippet updated: ${snippet.title}")
            }
        }
    }
    
    fun deleteSnippet(id: String) {
        scope.launch {
            val snippet = snippetRepository.getById(id)
            if (snippet != null) {
                fileStorageService.deleteSnippetFile(snippet.localPath)
                snippetRepository.delete(id)
                println("Snippet deleted: ${snippet.title}")
            }
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
            val snippet = snippetRepository.getById(id)
            if (snippet != null) {
                val updated = snippet.copy(
                    title = newTitle,
                    modifiedAt = System.currentTimeMillis()
                )
                snippetRepository.update(updated)
                println("Snippet renamed to: $newTitle")
            }
        }
    }
    
    fun renameFolder(id: String, newName: String) {
        scope.launch {
            val folder = folderRepository.getById(id)
            if (folder != null) {
                val updated = folder.copy(name = newName)
                folderRepository.update(updated)
                println("Folder renamed to: $newName")
            }
        }
    }
}
