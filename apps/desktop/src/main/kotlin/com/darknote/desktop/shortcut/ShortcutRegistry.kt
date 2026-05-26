package com.darknote.desktop.shortcut

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Immutable description of a key combination.
 */
data class KeyShortcut(
    val key: Key,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false
) {
    /**
     * Check if a [KeyEvent] matches this shortcut.
     */
    fun matches(event: KeyEvent): Boolean {
        return matches(
            key = event.key,
            ctrl = event.isCtrlPressed,
            shift = event.isShiftPressed,
            alt = event.isAltPressed
        )
    }

    /**
     * Check if raw key and modifier values match this shortcut.
     * Useful for testing without Compose Desktop runtime.
     */
    fun matches(key: Key, ctrl: Boolean = false, shift: Boolean = false, alt: Boolean = false): Boolean {
        return this.key == key && this.ctrl == ctrl && this.shift == shift && this.alt == alt
    }
}

/**
 * Registry for keyboard shortcuts. Supports scoped registration:
 * each call to [register] returns an unregister function.
 *
 * Usage:
 * ```kotlin
 * val registry = ShortcutRegistry()
 *
 * // In a composable:
 * DisposableEffect(Unit) {
 *     val unregister = registry.register(KeyShortcut(Key.S, ctrl = true)) { save(); true }
 *     onDispose { unregister() }
 * }
 * ```
 */
class ShortcutRegistry {

    private data class Binding(
        val shortcut: KeyShortcut,
        val action: () -> Boolean
    )

    private val bindings = mutableListOf<Binding>()

    /**
     * Register a shortcut. Returns a function that removes this binding when called.
     *
     * @param shortcut The key combination to match
     * @param action Callback to execute when the shortcut fires. Return `true` to consume the event.
     * @return Unregister function — call it to remove this binding
     */
    fun register(shortcut: KeyShortcut, action: () -> Boolean): () -> Unit {
        val binding = Binding(shortcut, action)
        bindings.add(binding)
        return { bindings.remove(binding) }
    }

    /**
     * Convenience: register with individual modifier parameters.
     */
    fun register(
        key: Key,
        ctrl: Boolean = false,
        shift: Boolean = false,
        alt: Boolean = false,
        action: () -> Boolean
    ): () -> Unit = register(KeyShortcut(key, ctrl, shift, alt), action)

    /**
     * Handle a key event. Iterates through registered bindings in registration order
     * and stops at the first one that returns `true`.
     *
     * @return `true` if the event was consumed, `false` otherwise
     */
    fun handleEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        return handleKeyDown(event.key, event.isCtrlPressed, event.isShiftPressed, event.isAltPressed)
    }

    /**
     * Handle a raw key-down event with primitive values.
     * Testable without Compose Desktop runtime.
     *
     * @return `true` if the event was consumed, `false` otherwise
     */
    fun handleKeyDown(key: Key, ctrl: Boolean = false, shift: Boolean = false, alt: Boolean = false): Boolean {
        for (binding in bindings) {
            if (binding.shortcut.matches(key, ctrl, shift, alt)) {
                if (binding.action()) return true
            }
        }
        return false
    }

    /** Remove all bindings. Useful for testing or reset. */
    fun clear() {
        bindings.clear()
    }

    /** Number of currently registered bindings. */
    val size: Int get() = bindings.size
}
