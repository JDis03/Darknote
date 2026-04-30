package com.darknote.persistence.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * JVM/Desktop implementation of database driver.
 */
actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(
            System.getProperty("user.home"),
            ".config/darknote/darknote.db"
        )
        databasePath.parentFile?.mkdirs()
        
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
        DarkNoteDatabase.Schema.create(driver)
        return driver
    }
}
