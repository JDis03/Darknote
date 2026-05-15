package com.darknote.persistence

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.darknote.persistence.database.DarkNoteDatabase
import com.darknote.persistence.database.DriverFactory
import java.io.File

class DatabaseDriverFactory : DriverFactory {
    override fun createDriver(): SqlDriver {
        val databasePath = getDatabasePath()
        val databaseFile = File(databasePath)
        
        // Create parent directory if doesn't exist
        databaseFile.parentFile?.mkdirs()
        
        val driver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")
        
        // Create schema if database doesn't exist
        if (!databaseFile.exists()) {
            DarkNoteDatabase.Schema.create(driver)
        }
        
        return driver
    }
    
    private fun getDatabasePath(): String {
        val userHome = System.getProperty("user.home")
        val appDataDir = when {
            System.getProperty("os.name").lowercase().contains("linux") -> {
                // XDG Base Directory Specification
                System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
            }
            System.getProperty("os.name").lowercase().contains("mac") -> {
                "$userHome/Library/Application Support"
            }
            else -> { // Windows
                System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
            }
        }
        
        return "$appDataDir/DarkNote/darknote.db"
    }
}
