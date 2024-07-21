package org.mobilenativefoundation.storex.paging.runtime.internal.logger.api

interface PagingLogger {
    fun verbose(message: String)
    fun debug(message: String)
    fun error(message: String, error: Throwable)
    fun warn(message: String)
}