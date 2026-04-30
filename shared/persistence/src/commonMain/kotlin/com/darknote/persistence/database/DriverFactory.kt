package com.darknote.persistence.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific database driver factory.
 */
expect class DriverFactory {
    fun createDriver(): SqlDriver
}
