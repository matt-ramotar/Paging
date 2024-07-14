package org.mobilenativefoundation.storex.paging.testUtils

import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger

class TestPagingLogger : PagingLogger {
    override fun verbose(message: String) {
        println(message)
    }

    override fun debug(message: String) {
        println(message)
    }

    override fun error(message: String, error: Throwable) {
        println(message)
        println(error)
    }

}