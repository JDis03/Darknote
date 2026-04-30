package com.darknote.core.model

import kotlinx.serialization.Serializable

/**
 * Represents a tag for cross-folder categorization.
 */
@Serializable
data class Tag(
    val id: String,
    val name: String,
    val color: String? = null,  // Hex color for UI
    val createdAt: Long
)
