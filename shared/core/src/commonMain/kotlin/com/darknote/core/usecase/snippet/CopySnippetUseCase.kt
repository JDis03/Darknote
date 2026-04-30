package com.darknote.core.usecase.snippet

import com.darknote.core.clipboard.ClipboardManager
import com.darknote.core.model.Snippet
import com.darknote.core.repository.SnippetRepository

/**
 * Use case for copying snippet content to clipboard with sanitization.
 * This is the core feature of the app.
 */
class CopySnippetUseCase(
    private val repository: SnippetRepository,
    private val clipboardManager: ClipboardManager
) {
    /**
     * Copy snippet to clipboard.
     * @param snippetId ID of snippet to copy
     * @param sanitize Whether to sanitize text for terminal (default: true)
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        snippetId: String,
        sanitize: Boolean = true
    ): Result<Unit> {
        val snippet = repository.getById(snippetId)
            ?: return Result.failure(IllegalArgumentException("Snippet not found"))

        return try {
            clipboardManager.copy(snippet.content, sanitize)
            repository.incrementUsageCount(snippetId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Copy snippet content directly without fetching from repository.
     * Useful when content is already available.
     */
    fun copyDirect(content: String, sanitize: Boolean = true): Result<Unit> {
        return try {
            clipboardManager.copy(content, sanitize)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
