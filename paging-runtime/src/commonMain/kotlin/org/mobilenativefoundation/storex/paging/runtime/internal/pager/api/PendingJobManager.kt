package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

/**
 * Interface for managing pending skip queue jobs.
 */
internal interface PendingJobManager<PageRequestKey : Any> {
    /**
     * Adds a new pending job.
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
    suspend fun updateExistingJob(key: PageRequestKey, inFlight: Boolean, completed: Boolean)

    /**
     * Checks if there are any pending jobs.
     *
     * @return True if there are pending jobs, false otherwise.
     */
    suspend fun hasPendingJobs(): Boolean

    /**
     * Clears all pending jobs.
     */
    suspend fun clearPendingJobs()

    /**
     * Returns the count of pending jobs.
     * @return The number of pending jobs.
     */
    suspend fun count(): Int
}