package com.darknote.android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class FabItem(val icon: ImageVector, val label: String) {
    SNIPPET(Icons.Default.TextSnippet, "Snippet"),
    TEXT(Icons.Default.Description, "Text"),
    CODE(Icons.Default.Code, "Code")
}

@Composable
fun ExpandableFab(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onItemClick: (FabItem) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isExpanded,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = modifier.padding(bottom = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            FabItem.entries.reversed().forEach { item ->
                SmallFloatingActionButton(
                    onClick = { onItemClick(item) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    FloatingActionButton(
        onClick = onToggle,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
            contentDescription = if (isExpanded) "Close" else "Add snippet",
            modifier = Modifier.size(24.dp)
        )
    }
}