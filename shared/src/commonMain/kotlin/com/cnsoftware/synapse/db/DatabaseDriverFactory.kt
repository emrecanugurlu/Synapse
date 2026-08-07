package com.cnsoftware.synapse.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Her platformun kendi SQLite sürücüsünü üretmesi için expect/actual sınıf.
 * Android -> AndroidSqliteDriver, iOS -> NativeSqliteDriver.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
