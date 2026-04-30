package com.darknote.desktop.ui.tree

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class TreeViewState(
    val items: List<TreeItemNode> = emptyList(),
    val selectedItemId: String? = null,
    val searchQuery: String = ""
)

data class TreeItemNode(
    val item: TreeItem,
    val depth: Int = 0,
    val isVisible: Boolean = true
)

@Composable
fun TreeView(
    state: TreeViewState,
    onItemClick: (TreeItem) -> Unit,
    onItemToggle: (TreeItem.FolderItem) -> Unit,
    onCreateSnippet: () -> Unit,
    onCreateFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TreeHeader(onCreateSnippet = onCreateSnippet, onCreateFolder = onCreateFolder)
        TreeSearchBar(query = state.searchQuery, onQueryChange = {})
        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        val visibleItems = state.items.filter { it.isVisible }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(visibleItems) { node ->
                val item = node.item
                val paddingStart = (node.depth * 16).dp

                Box(modifier = Modifier.padding(start = paddingStart)) {
                    when (item) {
                        is TreeItem.FolderItem -> TreeItemView(
                            item = item.copy(isSelected = item.id == state.selectedItemId),
                            onClick = { onItemClick(item) },
                            onToggleExpand = { onItemToggle(item) }
                        )
                        is TreeItem.SnippetItem -> TreeItemView(
                            item = item.copy(isSelected = item.id == state.selectedItemId),
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }

        TreeFooter(itemCount = state.items.count { it.item is TreeItem.SnippetItem })
    }
}

@Composable
private fun TreeHeader(onCreateSnippet: () -> Unit, onCreateFolder: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Snippets",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row {
            IconButton(onClick = onCreateFolder, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.CreateNewFolder, "New Folder", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onCreateSnippet, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, "New Snippet", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TreeSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search...", style = MaterialTheme.typography.bodySmall) },
        leadingIcon = {
            Icon(Icons.Default.Search, "Search", modifier = Modifier.size(18.dp))
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(40.dp),
        textStyle = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun TreeFooter(itemCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$itemCount items",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
