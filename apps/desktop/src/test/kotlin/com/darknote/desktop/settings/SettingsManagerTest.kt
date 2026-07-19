package com.darknote.desktop.settings

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Unit tests for SettingsManager.
 * Verifies:
 * - Default settings returned when file missing
 * - Round-trip save/load
 * - BACKWARD COMPATIBILITY with v1.0.0 format (lowercase theme strings)
 * - Tolerant handling of unknown theme values
 */
class SettingsManagerTest {

    private lateinit var tempDir: File
    private lateinit var manager: SettingsManager

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("darknote-test-").toFile()
        manager = TestableSettingsManager(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `load returns DEFAULT when file does not exist`() {
        val loaded = manager.load()
        assertEquals(AppSettings.DEFAULT.themeMode, loaded.themeMode)
        assertEquals(ThemeMode.SYSTEM, loaded.themeModeEnum)
    }

    @Test
    fun `save then load roundtrips theme mode`() {
        manager.setThemeMode(ThemeMode.DARK)
        val loaded = manager.load()
        assertEquals("dark", loaded.themeMode)
        assertEquals(ThemeMode.DARK, loaded.themeModeEnum)
    }

    @Test
    fun `save then load roundtrips light theme`() {
        manager.setThemeMode(ThemeMode.LIGHT)
        val loaded = manager.load()
        assertEquals(ThemeMode.LIGHT, loaded.themeModeEnum)
    }

    /**
     * REGRESSION TEST for v1.0.0 → v1.x settings migration.
     * v1.0.0 wrote `{"themeMode": "dark"}` (lowercase string).
     * New code must still parse this and resolve to ThemeMode.DARK,
     * not silently reset to SYSTEM.
     */
    @Test
    fun `load handles v1_0_0 lowercase format without losing user theme`() {
        File(tempDir, "settings.json").writeText("""{"themeMode":"dark"}""")
        val loaded = manager.load()
        assertEquals("dark", loaded.themeMode)
        assertEquals(ThemeMode.DARK, loaded.themeModeEnum)
    }

    @Test
    fun `load handles v1_0_0 light format`() {
        File(tempDir, "settings.json").writeText("""{"themeMode":"light"}""")
        val loaded = manager.load()
        assertEquals(ThemeMode.LIGHT, loaded.themeModeEnum)
    }

    @Test
    fun `load handles v1_0_0 system format`() {
        File(tempDir, "settings.json").writeText("""{"themeMode":"system"}""")
        val loaded = manager.load()
        assertEquals(ThemeMode.SYSTEM, loaded.themeModeEnum)
    }

    @Test
    fun `load falls back to SYSTEM for unknown theme value`() {
        File(tempDir, "settings.json").writeText("""{"themeMode":"neon"}""")
        val loaded = manager.load()
        assertEquals(ThemeMode.SYSTEM, loaded.themeModeEnum)
    }

    @Test
    fun `load recovers from corrupted JSON`() {
        File(tempDir, "settings.json").writeText("not valid json {{{")
        val loaded = manager.load()
        assertEquals(AppSettings.DEFAULT, loaded)
    }

    @Test
    fun `load handles extra unknown keys`() {
        File(tempDir, "settings.json").writeText(
            """{"themeMode":"light","futureSetting":"foo","version":2}"""
        )
        val loaded = manager.load()
        assertEquals(ThemeMode.LIGHT, loaded.themeModeEnum)
    }

    @Test
    fun `save returns true on success`() {
        val result = manager.save(AppSettings.DEFAULT)
        assertTrue(result)
        assertTrue(File(tempDir, "settings.json").exists())
    }

    @Test
    fun `setThemeMode returns true on success`() {
        assertTrue(manager.setThemeMode(ThemeMode.DARK))
    }

    @Test
    fun `setThemeMode persists lowercased keys`() {
        manager.setThemeMode(ThemeMode.DARK)
        manager.setThemeMode(ThemeMode.LIGHT)
        manager.setThemeMode(ThemeMode.SYSTEM)

        val file = File(tempDir, "settings.json")
        assertTrue("File should exist at ${file.absolutePath}", file.exists())
        val loaded = manager.load()
        assertEquals("system", loaded.themeMode)
        assertEquals(ThemeMode.SYSTEM, loaded.themeModeEnum)
    }
}

/**
 * Test-only SettingsManager that uses a custom directory instead of ~/.config/darknote.
 */
internal class TestableSettingsManager(private val testDir: File) : SettingsManager() {
    override fun resolveDir(): File = testDir
}