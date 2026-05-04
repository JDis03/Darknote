package com.darknote.desktop.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun EditorTabBar(
    tabs: List<EditorTab>,
    activeIndex: Int,
    onTabClick: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val activeColor = if (isActive) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    ScrollableTabRow(
        selectedTabIndex = activeIndex.coerceIn(-1, tabs.lastIndex),
        modifier = modifier.fillMaxWidth().height(36.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        edgePadding = 0.dp,
        divider = {},
        indicator = {}
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == activeIndex

            Surface(
                modifier = Modifier
                    .height(34.dp)
                    .padding(start = if (index == 0) 4.dp else 0.dp, top = 2.dp),
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                color = if (selected && isActive) activeColor
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shadowElevation = if (selected) 1.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onTabClick(index) }
                        .padding(start = 10.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected && isActive)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )

                    if (tab.isModified) {
                        Text(
                            "●",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { onTabClose(index) },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Close",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
