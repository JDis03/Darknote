package com.darknote.persistence.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AndroidDriverFactory(
    private val context: Context
) : DriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(DarkNoteDatabase.Schema, context, "darknote.db")
    }
}