package com.darknote.core.model

/**
 * Single source of truth for the snippet list filter inputs.
 * Shared across Android and Desktop to avoid duplicating ~50 lines of
 * filter logic in two ViewModels.
 */
data class FilterState(
    val snippets: List<Snippet> = emptyList(),
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val folderId: String? = null,
    val tag: String? = null,
)

enum class SortOrder {
    MODIFIED_DESC,
    CREATED_DESC,
    TITLE_ASC,
    MOST_USED,
}

/**
 * Pure function — testable in isolation, no Android/Desktop dependencies.
 * Applies all filter + sort logic in one pass.
 */
fun applyFilters(input: FilterState): List<Snippet> {
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
