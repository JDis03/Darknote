package com.darknote.persistence.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AndroidDriverFactory(
    private val context: Context
) : DriverFactory {
    override fun createDriver(): SqlDriver {
        // AndroidSqliteDriver with Schema automatically runs migrations
        // when the on-disk database version is older than Schema.version.
        // SQLDelight picks up .sqm files from the sqldelight source directory.
        return AndroidSqliteDriver(
            schema = DarkNoteDatabase.Schema,
            context = context,
            name = "darknote.db"
        )
    }
}