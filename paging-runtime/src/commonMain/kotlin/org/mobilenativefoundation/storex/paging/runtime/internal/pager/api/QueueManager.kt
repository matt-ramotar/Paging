package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api


import org.mobilenativefoundation.storex.paging.runtime.PagingAction

/**
 * Interface for managing queues of load parameters and pending jobs.
 *
 * @param PageRequestKey The type of the paging key, which must be Comparable.
 */
internal interface QueueManager<PageRequestKey: Any> {
    /**
     * The queue for append operations.
     */
    val appendQueue: LoadParamsQueue<PageRequestKey>

    /**
     * The queue for prepend operations.
     */
    val prependQueue: LoadParamsQueue<PageRequestKey>

    /**
     * Enqueues an append action.
     *
     * @param action The action to enqueue.
     */
    suspend fun enqueueAppend(action: PagingAction.Enqueue<PageRequestKey>)

    /**
     * Enqueues a prepend action.
     *
     * @param action The action to enqueue.
     */
    suspend fun enqueuePrepend(action: PagingAction.Enqueue<PageRequestKey>)

    /**
     * Adds a pending job.
     *
     * @param key The key for the job.
     * @param inFlight Whether the job is currently in flight.
     */
    suspend fun addPendingJob(key: PageRequestKey, inFlight: Boolean)

    /**
     * Updates an existing pending job.
     *
     * @param key The key for the job.
     * @param inFlight Whether the job is currently in flight.
     * @param completed Whether the job has completed.
     */
    suspend fun updateExistingPendingJob(key: PageRequestKey, inFlight: Boolean, completed: Boolean)

    /**
     * Checks if there are any pending jobs.
     *
     * @return True if there are pending jobs, false otherwise.
     */
    suspend fun hasPendingJobs(): Boolean

    /**
     * Clears all queues and pending jobs.
     */
    suspend fun clearQueues()
}

