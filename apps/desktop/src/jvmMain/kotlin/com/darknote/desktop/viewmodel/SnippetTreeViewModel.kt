package com.darknote.desktop.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.darknote.desktop.ui.tree.TreeItem
import com.darknote.desktop.ui.tree.TreeItemNode

class SnippetTreeViewModel {
    
    // Demo data
    private val folders = listOf(
        TreeItem.FolderItem("1", "Scripts", isExpanded = true, childCount = 3),
        TreeItem.FolderItem("2", "Database", isExpanded = false, childCount = 2),
        TreeItem.FolderItem("3", "Server Config", isExpanded = false, childCount = 1)
    )
    
    private val snippets = mapOf(
        "1" to listOf(
            TreeItem.SnippetItem("s1", "backup-database.sh", "bash", isFavorite = true),
            TreeItem.SnippetItem("s2", "deploy-server.sh", "bash"),
            TreeItem.SnippetItem("s3", "nginx-restart.sh", "bash")
        ),
        "2" to listOf(
            TreeItem.SnippetItem("s4", "mysql-optimize.sql", "sql"),
            TreeItem.SnippetItem("s5", "create-user.sql", "sql")
        ),
        "3" to listOf(
            TreeItem.SnippetItem("s6", "nginx.conf", "config", isFavorite = true)
        ),
        null to listOf(
            TreeItem.SnippetItem("s7", "quick-commands.txt", "text", isFavorite = true)
        )
    )
    
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
        
        folders.forEach { folder ->
            val isExpanded = expandedIds.contains(folder.id)
            nodes.add(TreeItemNode(
                item = folder.copy(isExpanded = isExpanded),
                depth = 0,
                isVisible = true
            ))
            
            // Show snippets if folder is expanded
            if (isExpanded) {
                snippets[folder.id]?.forEach { snippet ->
                    nodes.add(TreeItemNode(snippet, depth = 1, isVisible = true))
                }
            }
        }
        
        // Root snippets (no folder)
        snippets[null]?.forEach { snippet ->
            nodes.add(TreeItemNode(snippet, depth = 0, isVisible = true))
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
    
    fun createSnippet() {
        println("Create new snippet clicked")
    }
    
    fun createFolder() {
        println("Create new folder clicked")
    }
    
    fun getSelectedSnippet(): TreeItem.SnippetItem? {
        val selectedId = _selectedItemId.value ?: return null
        return snippets.values.flatten().find { it.id == selectedId }
    }
    
    companion object {
        fun create(): SnippetTreeViewModel = SnippetTreeViewModel()
    }
}
