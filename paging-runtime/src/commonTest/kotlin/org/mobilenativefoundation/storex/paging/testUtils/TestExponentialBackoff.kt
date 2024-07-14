package org.mobilenativefoundation.storex.paging.testUtils

import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ExponentialBackoff

internal class TestExponentialBackoff : ExponentialBackoff {
    override suspend fun execute(retryCount: Int, block: suspend () -> Unit) {
        block()
    }
}