package com.darknote.android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darknote.core.model.Folder
import com.darknote.android.SortOrder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterBar(
    folders: List<Folder>,
    selectedFolderId: String?,
    selectedTag: String?,
    showFavoritesOnly: Boolean,
    sortOrder: SortOrder,
    onFolderSelect: (String?) -> Unit,
    onFavoritesToggle: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onFolderLongClick: (Folder) -> Unit = { },
    onTagClear: () -> Unit = { },
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = showFavoritesOnly,
            onClick = onFavoritesToggle,
            label = { Text("Favorites") },
            leadingIcon = {
                Icon(
                    if (showFavoritesOnly) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        )

        folders.forEach { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = { onFolderSelect(if (selectedFolderId == folder.id) null else folder.id) },
                label = { Text(folder.name) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                modifier = Modifier.combinedClickable(
                    onClick = { onFolderSelect(if (selectedFolderId == folder.id) null else folder.id) },
                    onLongClick = { onFolderLongClick(folder) }
                )
            )
        }

        if (selectedTag != null) {
            FilterChip(
                selected = true,
                onClick = onTagClear,
                label = { Text("#$selectedTag") },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear tag filter",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }

        SortChip(currentSort = sortOrder, onSortChange = onSortOrderChange)
    }
}

@Composable
private fun SortChip(
    currentSort: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortLabel = when (currentSort) {
        SortOrder.MODIFIED_DESC -> "Recent"
        SortOrder.CREATED_DESC -> "Newest"
        SortOrder.TITLE_ASC -> "A-Z"
        SortOrder.MOST_USED -> "Top"
    }

    val nextSort = when (currentSort) {
        SortOrder.MODIFIED_DESC -> SortOrder.CREATED_DESC
        SortOrder.CREATED_DESC -> SortOrder.TITLE_ASC
        SortOrder.TITLE_ASC -> SortOrder.MOST_USED
        SortOrder.MOST_USED -> SortOrder.MODIFIED_DESC
    }

    AssistChip(
        onClick = { onSortChange(nextSort) },
        label = { Text(sortLabel) },
        leadingIcon = {
            Icon(
                Icons.Default.Tune,
                contentDescription = "Sort",
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        modifier = modifier
    )
}

private object AssistChipDefaults {
    val IconSize = 16.dp
}

private object FilterChipDefaults {
    val IconSize = 16.dp
}