package com.darknote.android

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darknote.core.clipboard.ClipboardManager
import com.darknote.core.model.Snippet
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.storage.FileStorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SnippetListViewModel(
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val storageService: FileStorageService,
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    private val _allSnippets = MutableStateFlow<List<Snippet>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _showFavoritesOnly = MutableStateFlow(false)

    val filteredSnippets = combine(
        _allSnippets,
        _searchQuery,
        _showFavoritesOnly
    ) { snippets, query, favoritesOnly ->
        snippets.filter { snippet ->
            val matchesQuery = query.isBlank() ||
                snippet.title.contains(query, ignoreCase = true) ||
                snippet.content.contains(query, ignoreCase = true) ||
                snippet.tags.any { it.contains(query, ignoreCase = true) }

            val matchesFavorite = !favoritesOnly || snippet.isFavorite

            matchesQuery && matchesFavorite
        }.sortedByDescending { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _copiedSnippetId = MutableStateFlow<String?>(null)
    val copiedSnippetId: StateFlow<String?> = _copiedSnippetId.asStateFlow()

    private val _showFavoritesOnlyState = mutableStateOf(false)
    val showFavoritesOnly: State<Boolean> = _showFavoritesOnlyState

    init {
        loadSnippets()
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

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowFavorites() {
        val newValue = !_showFavoritesOnlyState.value
        _showFavoritesOnly.value = newValue
        _showFavoritesOnlyState.value = newValue
    }

    fun copySnippet(snippet: Snippet) {
        viewModelScope.launch {
            val content = storageService.loadSnippetContent(snippet.localPath).getOrNull()
                ?: snippet.content

            clipboardManager.copy(content, sanitize = true)
            _copiedSnippetId.value = snippet.id

            kotlinx.coroutines.delay(2000)
            _copiedSnippetId.value = null
        }
    }

    fun toggleFavorite(snippet: Snippet) {
        viewModelScope.launch {
            val updated = snippet.copy(isFavorite = !snippet.isFavorite)
            snippetRepository.update(updated)
        }
    }
}