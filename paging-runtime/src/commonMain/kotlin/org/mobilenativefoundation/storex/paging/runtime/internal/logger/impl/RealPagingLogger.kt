package org.mobilenativefoundation.storex.paging.runtime.internal.logger.impl

import co.touchlab.kermit.Logger
import org.mobilenativefoundation.storex.paging.runtime.Severity
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger

class RealPagingLogger(
    private val severity: Severity
) : PagingLogger {
    override fun debug(message: String) {
        if (severity.ordinal >= Severity.Debug.ordinal) {
            Logger.d("storex/paging") { message }
        }
    }

    override fun error(message: String, error: Throwable) {
        if (severity.ordinal >= Severity.Error.ordinal) {
            Logger.e("storex/paging", error) { message }
        }
    }

    override fun verbose(message: String) {
        if (severity.ordinal >= Severity.Verbose.ordinal) {
            Logger.v("storex/paging") { message }
        }
    }
}

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