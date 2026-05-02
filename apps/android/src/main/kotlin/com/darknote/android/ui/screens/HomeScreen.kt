@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)

package com.darknote.android.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.darknote.android.CreateSnippetState
import com.darknote.android.SnackbarData
import com.darknote.android.SnippetListViewModel
import com.darknote.android.ui.components.CreateSnippetSheet
import com.darknote.android.ui.components.EditSnippetSheet
import com.darknote.android.ui.components.EmptyStateView
import com.darknote.android.ui.components.ExpandableFab
import com.darknote.android.ui.components.FilterBar
import com.darknote.android.ui.components.SearchBar
import com.darknote.android.ui.components.SnippetCard
import com.darknote.android.ui.components.SnippetContextMenu
import com.darknote.android.ui.components.SnippetDetailSheet
import com.darknote.android.ui.components.SwipeToDismissBox
import com.darknote.core.model.Snippet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SnippetListViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snippets by viewModel.filteredSnippets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val copiedSnippetId by viewModel.copiedSnippetId.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarData by viewModel.snackbarData.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val createState by viewModel.createState.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var isFabExpanded by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showDetailSnippet by remember { mutableStateOf<Snippet?>(null) }
    var editSnippet by remember { mutableStateOf<Snippet?>(null) }
    var contextMenuSnippet by remember { mutableStateOf<Snippet?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    LaunchedEffect(snackbarData) {
        val data = snackbarData ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = data.message,
            actionLabel = data.actionLabel,
            duration = SnackbarDuration.Short
        )
        when (result) {
            SnackbarResult.ActionPerformed -> data.action?.invoke()
            SnackbarResult.Dismissed -> { /* dismissed */ }
        }
        viewModel.clearSnackbar()
    }

    LaunchedEffect(createState) {
        if (createState is CreateSnippetState.Created) {
            showCreateSheet = false
            viewModel.clearCreateState()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "DarkNote",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary
                )
            }
        },
        floatingActionButton = {
            ExpandableFab(
                isExpanded = isFabExpanded,
                onToggle = { isFabExpanded = !isFabExpanded },
                onItemClick = {
                    showCreateSheet = true
                    isFabExpanded = false
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClear = viewModel::clearSearch,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    recentSearches = recentSearches,
                    onRecentSearchClick = { viewModel.onSearchQueryChange(it) },
                    onRemoveRecentSearch = { viewModel.removeRecentSearch(it) }
                )

                FilterBar(
                    folders = folders,
                    selectedFolderId = selectedFolderId,
                    showFavoritesOnly = showFavoritesOnly,
                    sortOrder = sortOrder,
                    onFolderSelect = viewModel::selectFolder,
                    onFavoritesToggle = viewModel::toggleShowFavorites,
                    onSortOrderChange = viewModel::setSortOrder
                )

                if (snippets.isEmpty()) {
                    EmptyStateView(
                        isSearch = searchQuery.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = snippets, key = { snippet -> snippet.id }) { snippet ->
                            val isCopied = copiedSnippetId == snippet.id

                            SwipeToDismissBox(
                                onSwipeLeft = { viewModel.deleteSnippet(snippet) },
                                onSwipeRight = { viewModel.toggleFavorite(snippet) },
                                enableSwipeLeft = true,
                                enableSwipeRight = true
                            ) {
                                SnippetCard(
                                    snippet = snippet,
                                    isCopied = isCopied,
                                    onCopy = { viewModel.copySnippet(snippet) },
                                    onToggleFavorite = { viewModel.toggleFavorite(snippet) },
                                    onClick = { showDetailSnippet = snippet },
                                    onLongClick = {
                                        contextMenuSnippet = snippet
                                        showContextMenu = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            val snippet = contextMenuSnippet
            if (snippet != null) {
                SnippetContextMenu(
                    isFavorite = snippet.isFavorite,
                    onEdit = {
                        showContextMenu = false
                        editSnippet = snippet
                    },
                    onCopy = {
                        showContextMenu = false
                        viewModel.copySnippet(snippet)
                    },
                    onCopyRaw = {
                        showContextMenu = false
                        viewModel.copyRawSnippet(snippet)
                    },
                    onToggleFavorite = {
                        showContextMenu = false
                        viewModel.toggleFavorite(snippet)
                    },
                    onShare = {
                        showContextMenu = false
                        viewModel.shareSnippet(snippet, context)
                    },
                    onDelete = {
                        showContextMenu = false
                        viewModel.deleteSnippet(snippet)
                    }
                )
            }
        }
    }

    val detailSnippet = showDetailSnippet
    if (detailSnippet != null) {
        val folderName = folders.find { it.id == detailSnippet.folderId }?.name
        SnippetDetailSheet(
            snippet = detailSnippet,
            onDismiss = { showDetailSnippet = null },
            onEdit = { s -> editSnippet = s },
            onCopy = { viewModel.copySnippet(it) },
            onCopyRaw = { viewModel.copyRawSnippet(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDelete = { s ->
                viewModel.deleteSnippet(s)
                showDetailSnippet = null
            },
            onShare = { viewModel.shareSnippet(it, context) },
            folderName = folderName
        )
    }

    if (showCreateSheet) {
        CreateSnippetSheet(
            folders = folders,
            onDismiss = {
                showCreateSheet = false
                viewModel.clearCreateState()
            },
            onCreate = { title, content, language, tags, folderId ->
                viewModel.createSnippet(title, content, language, tags, folderId)
            }
        )
    }

    val editing = editSnippet
    if (editing != null) {
        EditSnippetSheet(
            snippet = editing,
            folders = folders,
            onDismiss = { editSnippet = null },
            onSave = { s ->
                viewModel.updateSnippet(s)
                editSnippet = null
            }
        )
    }
}