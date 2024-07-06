package org.mobilenativefoundation.storex.paging.runtime

interface PagingLogger {
    fun debug(message: String)
    fun error(message: String, throwable: Throwable? = null)
}