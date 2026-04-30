package com.darknote.desktop.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.darknote.desktop.ui.tree.TreeItem
import com.darknote.desktop.ui.tree.TreeItemNode

class SnippetTreeViewModel {
    private val _selectedItemId = mutableStateOf<String?>(null)
    val selectedItemId: State<String?> = _selectedItemId

    private val _expandedFolderIds = mutableStateOf<Set<String>>(emptySet())
    val expandedFolderIds: State<Set<String>> = _expandedFolderIds

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _folders = mutableStateOf(listOf(
        TreeItem.FolderItem("1", "Scripts", isExpanded = true, childCount = 3),
        TreeItem.FolderItem("2", "Database", isExpanded = false, childCount = 2),
        TreeItem.FolderItem("3", "Server Config", isExpanded = false, childCount = 1)
    ))

    private val _snippets = mutableStateOf(mapOf(
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
    ))

    fun getVisibleItems(): List<TreeItemNode> {
        val nodes = mutableListOf<TreeItemNode>()
        val expandedIds = _expandedFolderIds.value

        _folders.value.forEach { folder ->
            nodes.add(TreeItemNode(folder, depth = 0, isVisible = true))

            if (folder.isExpanded && expandedIds.contains(folder.id)) {
                _snippets.value[folder.id]?.forEach { snippet ->
                    nodes.add(TreeItemNode(snippet, depth = 1, isVisible = true))
                }
            }
        }

        _snippets.value[null]?.forEach { snippet ->
            nodes.add(TreeItemNode(snippet, depth = 0, isVisible = true))
        }

        return nodes
    }

    fun selectItem(itemId: String) {
        _selectedItemId.value = itemId
    }

    fun toggleFolder(folderId: String) {
        val current = _expandedFolderIds.value
        _expandedFolderIds.value = if (current.contains(folderId)) current - folderId else current + folderId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createSnippet() = println("Create new snippet")
    fun createFolder() = println("Create new folder")

    fun getSelectedSnippet(): TreeItem.SnippetItem? {
        val selectedId = _selectedItemId.value ?: return null
        return _snippets.value.values.flatten().find { it.id == selectedId }
    }

    companion object {
        fun create(): SnippetTreeViewModel = SnippetTreeViewModel()
    }
}
