package org.mobilenativefoundation.storex.paging.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.mobilenativefoundation.storex.paging.PagingDb


actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(PagingDb.Schema, "paging.db")
    }
}