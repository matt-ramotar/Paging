package org.mobilenativefoundation.storex.paging.persistence.api

/**
 * Represents the result of a data operation, either successful or an error.
 */
sealed class PersistenceResult<out T> {
    data object Skipped: PersistenceResult<Nothing>()
    data class Success<T>(val data: T) : PersistenceResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) :
        PersistenceResult<Nothing>()
}