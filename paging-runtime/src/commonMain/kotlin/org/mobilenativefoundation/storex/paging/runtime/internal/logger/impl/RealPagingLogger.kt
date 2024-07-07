package org.mobilenativefoundation.storex.paging.runtime.internal.logger.impl

import co.touchlab.kermit.Logger
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingConfig
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger

class RealPagingLogger<Id : Identifier<*>, K : Any>(
    private val pagingConfig: PagingConfig<Id, K>
) : PagingLogger {
    override fun debug(message: String) {
        if (pagingConfig.debug) {
            Logger.d("storex/paging") { message }
        }
    }
}