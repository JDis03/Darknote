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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SnippetListViewModel(
    private val snippetRepository: SnippetRepository,
    private val folderRepository: FolderRepository,
    private val storageService: FileStorageService,
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    private val _allSnippets = MutableStateFlow<List<Snippet>>(emptyList())
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showFavoritesOnly = mutableStateOf(false)
    val showFavoritesOnly: State<Boolean> = _showFavoritesOnly

    private val _copiedSnippetId = MutableStateFlow<String?>(null)
    val copiedSnippetId: StateFlow<String?> = _copiedSnippetId.asStateFlow()

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
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun copySnippet(snippet: Snippet) {
        viewModelScope.launch {
            val content = storageService.loadSnippetContent(snippet.id).getOrNull() 
                ?: snippet.content
            
            clipboardManager.copyToClipboard(content)
            _copiedSnippetId.value = snippet.id
            
            // Clear copied indicator after 2 seconds
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