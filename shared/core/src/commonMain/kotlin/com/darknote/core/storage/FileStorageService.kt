package com.darknote.core.storage

import com.darknote.core.model.Snippet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Service for managing snippet files on disk.
 * Stores content in .txt files and metadata in database.
 */
class FileStorageService(
    private val baseDirectory: File
) {
    init {
        baseDirectory.mkdirs()
    }

    /**
     * Save snippet content to file.
     */
    suspend fun saveSnippetContent(snippet: Snippet): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val file = File(baseDirectory, snippet.localPath)
                file.parentFile?.mkdirs()
                file.writeText(snippet.content)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load snippet content from file.
     */
    suspend fun loadSnippetContent(localPath: String): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                val file = File(baseDirectory, localPath)
                if (file.exists()) {
                    Result.success(file.readText())
                } else {
                    Result.failure(IllegalStateException("File not found: $localPath"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete snippet file.
     */
    suspend fun deleteSnippetFile(localPath: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val file = File(baseDirectory, localPath)
                if (file.exists()) {
                    file.delete()
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if snippet file exists.
     */
    fun snippetExists(localPath: String): Boolean {
        return File(baseDirectory, localPath).exists()
    }

    /**
     * Generate safe filename from title.
     */
    fun generateSafePath(title: String): String {
        val safeName = title
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(50)
            .lowercase()
        return "snippets/${safeName}_${System.currentTimeMillis()}.txt"
    }
}
