package com.darknote.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterStateTest {

    private fun snippet(
        id: String,
        title: String,
        content: String = "",
        tags: List<String> = emptyList(),
        folderId: String? = null,
        isFavorite: Boolean = false,
        language: String? = null,
        modifiedAt: Long = 0L,
        createdAt: Long = 0L
    ) = Snippet(
        id = id,
        title = title,
        content = content,
        folderId = folderId,
        tags = tags,
        isFavorite = isFavorite,
        language = language,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        localPath = "snippets/$id.txt"
    )

    @Test
    fun `empty state returns empty list`() {
        val result = applyFilters(FilterState())
        assertEquals(emptyList(), result)
    }

    @Test
    fun `blanks query matches everything`() {
        val snippets = listOf(
            snippet("1", "foo"),
            snippet("2", "bar")
        )
        val result = applyFilters(FilterState(snippets = snippets, query = ""))
        assertEquals(snippets, result)
    }

    @Test
    fun `query matches title case-insensitively`() {
        val snippets = listOf(
            snippet("1", "MySQL Backup"),
            snippet("2", "Redis Cache")
        )
        val result = applyFilters(FilterState(snippets = snippets, query = "mysql"))
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `query matches content`() {
        val snippets = listOf(
            snippet("1", "Untitled", content = "SELECT * FROM users"),
            snippet("2", "Untitled", content = "ls -la")
        )
        val result = applyFilters(FilterState(snippets = snippets, query = "select"))
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `query matches tags`() {
        val snippets = listOf(
            snippet("1", "Snippet", tags = listOf("docker", "backup")),
            snippet("2", "Snippet", tags = listOf("redis"))
        )
        val result = applyFilters(FilterState(snippets = snippets, query = "docker"))
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `query matches language`() {
        val snippets = listOf(
            snippet("1", "Snippet", language = "python"),
            snippet("2", "Snippet", language = "rust")
        )
        val result = applyFilters(FilterState(snippets = snippets, query = "python"))
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `favoritesOnly filter excludes non-favorites`() {
        val snippets = listOf(
            snippet("1", "Fav", isFavorite = true),
            snippet("2", "NotFav", isFavorite = false)
        )
        val result = applyFilters(FilterState(snippets = snippets, favoritesOnly = true))
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `folderId filter scopes to folder`() {
        val snippets = listOf(
            snippet("1", "InScripts", folderId = "scripts"),
            snippet("2", "InDatabase", folderId = "database")
        )
        val result = applyFilters(FilterState(snippets = snippets, folderId = "scripts"))
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `tag filter matches case-insensitively`() {
        val snippets = listOf(
            snippet("1", "Snippet", tags = listOf("Backend")),
            snippet("2", "Snippet", tags = listOf("Frontend"))
        )
        val result = applyFilters(FilterState(snippets = snippets, tag = "backend"))
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `MODIFIED_DESC sorts by modifiedAt descending`() {
        val snippets = listOf(
            snippet("1", "Old", modifiedAt = 1000L),
            snippet("2", "New", modifiedAt = 3000L),
            snippet("3", "Mid", modifiedAt = 2000L)
        )
        val result = applyFilters(FilterState(snippets = snippets))
        assertEquals(listOf("2", "3", "1"), result.map { it.id })
    }

    @Test
    fun `TITLE_ASC sorts case-insensitively`() {
        val snippets = listOf(
            snippet("1", "banana"),
            snippet("2", "Apple"),
            snippet("3", "cherry")
        )
        val result = applyFilters(FilterState(snippets = snippets, sortOrder = SortOrder.TITLE_ASC))
        assertEquals(listOf("2", "1", "3"), result.map { it.id })
    }

    @Test
    fun `multiple filters combine with AND`() {
        val snippets = listOf(
            snippet("1", "Match", content = "python", folderId = "f1", isFavorite = true),
            snippet("2", "Match", content = "python", folderId = "f1", isFavorite = false),
            snippet("3", "Match", content = "python", folderId = "f2", isFavorite = true),
            snippet("4", "Other", content = "rust", folderId = "f1", isFavorite = true)
        )
        val result = applyFilters(
            FilterState(
                snippets = snippets,
                query = "match",
                favoritesOnly = true,
                folderId = "f1"
            )
        )
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `null folderId means all folders`() {
        val snippets = listOf(
            snippet("1", "A", folderId = "f1"),
            snippet("2", "B", folderId = "f2"),
            snippet("3", "C", folderId = null)
        )
        val result = applyFilters(FilterState(snippets = snippets, folderId = null))
        assertEquals(3, result.size)
    }

    @Test
    fun `empty snippets list returns empty`() {
        val result = applyFilters(FilterState(snippets = emptyList(), query = "anything"))
        assertEquals(emptyList(), result)
    }

    @Test
    fun `snippet with no matching fields excluded`() {
        val snippets = listOf(
            snippet("1", "Alpha", content = "x", tags = listOf("y"), language = "kotlin"),
            snippet("2", "Beta", content = "z", tags = listOf("w"), language = "rust")
        )
        val result = applyFilters(FilterState(snippets = snippets, query = "alpha"))
        assertTrue(result.any { it.id == "1" })
        assertTrue(result.none { it.id == "2" })
    }
}
