package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api


import org.mobilenativefoundation.storex.paging.runtime.Action

/**
 * Interface for managing queues of load parameters and pending jobs.
 *
 * @param K The type of the paging key, which must be Comparable.
 */
internal interface QueueManager<K : Comparable<K>> {
    /**
     * The queue for append operations.
     */
    val appendQueue: LoadParamsQueue<K>

    /**
     * The queue for prepend operations.
     */
    val prependQueue: LoadParamsQueue<K>

    /**
     * Enqueues an append action.
     *
     * @param action The action to enqueue.
     */
    suspend fun enqueueAppend(action: Action.Enqueue<K>)

    /**
     * Enqueues a prepend action.
     *
     * @param action The action to enqueue.
     */
    suspend fun enqueuePrepend(action: Action.Enqueue<K>)

    /**
     * Adds a pending job.
     *
     * @param key The key for the job.
     * @param inFlight Whether the job is currently in flight.
     */
    suspend fun addPendingJob(key: K, inFlight: Boolean)

    /**
     * Updates an existing pending job.
     *
     * @param key The key for the job.
     * @param inFlight Whether the job is currently in flight.
     * @param completed Whether the job has completed.
     */
    suspend fun updateExistingPendingJob(key: K, inFlight: Boolean, completed: Boolean)

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

