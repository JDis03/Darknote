package com.darknote.desktop.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class EditorTab(
    val snippetId: String,
    val title: String,
    val isModified: Boolean = false
)

class EditorState {
    val tabs = mutableListOf<EditorTab>()
    var activeTabIndex by mutableIntStateOf(-1)

    var isSplit by mutableStateOf(false)
    val leftTabs = mutableListOf<EditorTab>()
    val rightTabs = mutableListOf<EditorTab>()
    var leftActiveIndex by mutableIntStateOf(-1)
    var rightActiveIndex by mutableIntStateOf(-1)

    val activeTab: EditorTab?
        get() = if (isSplit) {
            null // No single active tab in split mode
        } else {
            tabs.getOrNull(activeTabIndex)
        }

    fun openTab(snippetId: String, title: String) {
        val existing = tabs.indexOfFirst { it.snippetId == snippetId }
        if (existing >= 0) {
            activeTabIndex = existing
            return
        }
        tabs.add(EditorTab(snippetId, title))
        activeTabIndex = tabs.lastIndex
    }

    fun closeTab(index: Int) {
        if (index < 0 || index >= tabs.size) return
        tabs.removeAt(index)
        if (tabs.isEmpty()) {
            activeTabIndex = -1
        } else if (activeTabIndex >= tabs.size) {
            activeTabIndex = tabs.lastIndex
        }
    }

    fun closeTabById(snippetId: String) {
        val index = tabs.indexOfFirst { it.snippetId == snippetId }
        if (index >= 0) closeTab(index)
    }

    fun setTabModified(snippetId: String, modified: Boolean) {
        tabs.indexOfFirst { it.snippetId == snippetId }.takeIf { it >= 0 }?.let {
            tabs[it] = tabs[it].copy(isModified = modified)
        }
    }

    fun updateTabTitle(snippetId: String, newTitle: String) {
        tabs.indexOfFirst { it.snippetId == snippetId }.takeIf { it >= 0 }?.let {
            tabs[it] = tabs[it].copy(title = newTitle)
        }
    }

    fun splitRight() {
        if (tabs.isEmpty()) return
        isSplit = true
        leftTabs.clear()
        leftTabs.addAll(tabs)
        rightTabs.clear()
        leftActiveIndex = activeTabIndex
        rightActiveIndex = -1
    }

    fun unsplit() {
        isSplit = false
        tabs.clear()
        tabs.addAll(leftTabs)
        tabs.addAll(rightTabs.filter { rt -> leftTabs.none { it.snippetId == rt.snippetId } })
        activeTabIndex = leftActiveIndex.coerceAtMost(tabs.lastIndex)
        leftTabs.clear()
        rightTabs.clear()
    }
}
