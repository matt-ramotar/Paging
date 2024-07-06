package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

/**
 * Represents a job that is pending in the skip queue.
 */
internal data class PendingSkipQueueJob<K : Any>(
    val key: K,
    val inFlight: Boolean
)