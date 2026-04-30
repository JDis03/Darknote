package com.darknote.core.usecase.snippet

import com.darknote.core.model.Snippet
import com.darknote.core.repository.SnippetRepository

/**
 * Use case for creating a new snippet.
 */
class CreateSnippetUseCase(
    private val repository: SnippetRepository
) {
    suspend operator fun invoke(
        title: String,
        content: String,
        folderId: String? = null,
        language: String? = null,
        tags: List<String> = emptyList()
    ): Result<Snippet> {
        // Validation
        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Title cannot be empty"))
        }

        val snippet = Snippet(
            id = generateId(),
            title = title.trim(),
            content = content,
            folderId = folderId,
            tags = tags,
            language = language?.lowercase(),
            isFavorite = false,
            createdAt = currentTimeMillis(),
            modifiedAt = currentTimeMillis(),
            localPath = generateLocalPath(title)
        )

        return repository.create(snippet).map { snippet }
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString()
    private fun currentTimeMillis(): Long = System.currentTimeMillis()
    private fun generateLocalPath(title: String): String {
        val safeName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
        return "snippets/${safeName}_${currentTimeMillis()}.txt"
    }
}
