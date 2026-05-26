package com.darknote.desktop.shortcut

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ShortcutRegistry.
 *
 * Uses [ShortcutRegistry.handleKeyDown] for testability without Compose Desktop runtime.
 */
class ShortcutRegistryTest {

    private lateinit var registry: ShortcutRegistry

    @Before
    fun setup() {
        registry = ShortcutRegistry()
    }

    @Test
    fun `register adds binding`() {
        registry.register(KeyShortcut(Key.S, ctrl = true)) { true }
        assertEquals(1, registry.size)
    }

    @Test
    fun `register multiple adds multiple bindings`() {
        registry.register(KeyShortcut(Key.S, ctrl = true)) { true }
        registry.register(KeyShortcut(Key.N, ctrl = true)) { true }
        registry.register(KeyShortcut(Key.F, ctrl = true)) { true }
        assertEquals(3, registry.size)
    }

    @Test
    fun `unregister removes binding`() {
        val unregister = registry.register(KeyShortcut(Key.S, ctrl = true)) { true }
        assertEquals(1, registry.size)
        unregister()
        assertEquals(0, registry.size)
    }

    @Test
    fun `unregister multiple times is idempotent`() {
        val unregister = registry.register(KeyShortcut(Key.S, ctrl = true)) { true }
        unregister()
        unregister() // should not throw
        assertEquals(0, registry.size)
    }

    @Test
    fun `clear removes all bindings`() {
        registry.register(KeyShortcut(Key.S, ctrl = true)) { true }
        registry.register(KeyShortcut(Key.N, ctrl = true)) { true }
        registry.clear()
        assertEquals(0, registry.size)
    }

    @Test
    fun `handleKeyDown returns true when shortcut matches`() {
        var executed = false
        registry.register(KeyShortcut(Key.S, ctrl = true)) {
            executed = true
            true
        }
        val consumed = registry.handleKeyDown(Key.S, ctrl = true)
        assertTrue(consumed)
        assertTrue(executed)
    }

    @Test
    fun `handleKeyDown returns false when no shortcut registered`() {
        val consumed = registry.handleKeyDown(Key.S, ctrl = true)
        assertFalse(consumed)
    }

    @Test
    fun `handleKeyDown returns false when key does not match`() {
        registry.register(KeyShortcut(Key.S, ctrl = true)) { true }
        val consumed = registry.handleKeyDown(Key.N, ctrl = true)
        assertFalse(consumed)
    }

    @Test
    fun `handleKeyDown returns false when modifier does not match`() {
        registry.register(KeyShortcut(Key.S, ctrl = true)) { true }
        val consumed = registry.handleKeyDown(Key.S) // no ctrl
        assertFalse(consumed)
    }

    @Test
    fun `handleKeyDown with shift modifier`() {
        var executed = false
        registry.register(KeyShortcut(Key.F, ctrl = true, shift = true)) {
            executed = true
            true
        }
        assertTrue(registry.handleKeyDown(Key.F, ctrl = true, shift = true))
        assertTrue(executed)
    }

    @Test
    fun `handleKeyDown stops at first matching binding`() {
        var firstExecuted = false
        var secondExecuted = false
        registry.register(KeyShortcut(Key.S, ctrl = true)) {
            firstExecuted = true
            true // consumed
        }
        registry.register(KeyShortcut(Key.S, ctrl = true)) {
            secondExecuted = true
            true
        }
        assertTrue(registry.handleKeyDown(Key.S, ctrl = true))
        assertTrue(firstExecuted)
        assertFalse(secondExecuted) // second should not execute
    }

    @Test
    fun `action returning false allows next binding`() {
        var firstExecuted = false
        var secondExecuted = false
        registry.register(KeyShortcut(Key.S, ctrl = true)) {
            firstExecuted = true
            false // not consumed
        }
        registry.register(KeyShortcut(Key.S, ctrl = true)) {
            secondExecuted = true
            true // consumed
        }
        assertTrue(registry.handleKeyDown(Key.S, ctrl = true))
        assertTrue(firstExecuted)
        assertTrue(secondExecuted)
    }

    @Test
    fun `KeyShortcut matches with correct modifiers`() {
        val shortcut = KeyShortcut(Key.N, ctrl = true)
        assertTrue(shortcut.matches(Key.N, ctrl = true))
        assertFalse(shortcut.matches(Key.N)) // no ctrl
        assertFalse(shortcut.matches(Key.S, ctrl = true)) // wrong key
    }

    @Test
    fun `KeyShortcut matches with shift`() {
        val shortcut = KeyShortcut(Key.F, ctrl = true, shift = true)
        assertTrue(shortcut.matches(Key.F, ctrl = true, shift = true))
        assertFalse(shortcut.matches(Key.F, ctrl = true)) // missing shift
        assertFalse(shortcut.matches(Key.F, shift = true)) // missing ctrl
    }

    @Test
    fun `convenience register with individual params`() {
        var executed = false
        registry.register(Key.S, ctrl = true) {
            executed = true
            true
        }
        assertTrue(registry.handleKeyDown(Key.S, ctrl = true))
        assertTrue(executed)
    }

    @Test
    fun `empty registry handleKeyDown returns false`() {
        val consumed = registry.handleKeyDown(Key.Q, ctrl = true)
        assertFalse(consumed)
    }
}
