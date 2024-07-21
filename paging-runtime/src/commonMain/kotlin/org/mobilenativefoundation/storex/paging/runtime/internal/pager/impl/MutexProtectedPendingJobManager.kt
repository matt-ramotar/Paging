package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PendingJobManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PendingSkipQueueJob

/**
 * Implementation of PendingJobManager using a mutex-protected map.
 */
class MutexProtectedPendingJobManager<PageRequestKey : Any> : PendingJobManager<PageRequestKey> {
    private val pendingJobs = HashMap<PageRequestKey, PendingSkipQueueJob<PageRequestKey>>()
    private val mutex = Mutex()

    override suspend fun addPendingJob(key: PageRequestKey, inFlight: Boolean) = mutex.withLock {
        pendingJobs[key] = PendingSkipQueueJob(key, inFlight)
    }

    override suspend fun updateExistingJob(key: PageRequestKey, inFlight: Boolean, completed: Boolean) =
        mutex.withLock {
            if (key !in pendingJobs) return@withLock
            if (completed) {
                pendingJobs.remove(key)
            } else {
                pendingJobs[key] = PendingSkipQueueJob(key, inFlight)
            }
        }

    override suspend fun hasPendingJobs(): Boolean = mutex.withLock { pendingJobs.isNotEmpty() }

    override suspend fun clearPendingJobs() = mutex.withLock { pendingJobs.clear() }

    override suspend fun count() = mutex.withLock {
        pendingJobs.size
    }
}