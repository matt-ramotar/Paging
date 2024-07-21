package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

/**
 * Represents a job that is pending in the skip queue.
 */
internal data class PendingSkipQueueJob<PageRequestKey : Any>(
    val key: PageRequestKey,
    val inFlight: Boolean
)