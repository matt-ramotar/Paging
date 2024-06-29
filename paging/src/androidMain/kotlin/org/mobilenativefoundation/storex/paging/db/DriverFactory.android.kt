package org.mobilenativefoundation.storex.paging.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.mobilenativefoundation.storex.paging.PagingDb

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(PagingDb.Schema, context, "paging.db")
    }
}