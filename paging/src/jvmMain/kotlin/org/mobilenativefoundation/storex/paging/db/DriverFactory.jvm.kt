package org.mobilenativefoundation.storex.paging.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.mobilenativefoundation.storex.paging.PagingDb

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PagingDb.Schema.create(driver)
        return driver
    }
}