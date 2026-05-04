package com.darknote.core.model

import kotlinx.serialization.Serializable

/**
 * Represents a folder for organizing snippets.
 */
@Serializable
data class Folder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long
)
