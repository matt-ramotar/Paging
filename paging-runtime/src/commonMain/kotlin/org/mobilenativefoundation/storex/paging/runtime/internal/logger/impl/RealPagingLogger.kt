package org.mobilenativefoundation.storex.paging.runtime.internal.logger.impl

import co.touchlab.kermit.Logger
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingConfig
import org.mobilenativefoundation.storex.paging.runtime.Severity
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger

class RealPagingLogger<Id : Identifier<*>, K : Any>(
    private val pagingConfig: PagingConfig<Id, K>
) : PagingLogger {
    override fun debug(message: String) {
        if (pagingConfig.logging.ordinal >= Severity.Debug.ordinal) {
            Logger.d("storex/paging") { message }
        }
    }

    override fun error(message: String, error: Throwable) {
        if (pagingConfig.logging.ordinal >= Severity.Error.ordinal) {
            Logger.e("storex/paging", error) { message }
        }
    }

    override fun verbose(message: String) {
        if (pagingConfig.logging.ordinal >= Severity.Verbose.ordinal) {
            Logger.v("storex/paging") { message }
        }
    }
}