package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.Action
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadParamsQueue
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PendingJobManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.QueueManager

/**
 * Implementation of QueueManager that manages load parameter queues and pending jobs.
 *
 * @param K The type of the paging key, which must be Comparable.
 */
internal class RealQueueManager<K : Comparable<K>> : QueueManager<K> {
    override val appendQueue: LoadParamsQueue<K> = RealLoadParamsQueue()
    override val prependQueue: LoadParamsQueue<K> = RealLoadParamsQueue()

    private val pendingJobManager: PendingJobManager<K> = MutexProtectedPendingJobManager()
    private val mutex = Mutex()

    override suspend fun enqueueAppend(action: Action.Enqueue<K>) = mutex.withLock {
        val element = LoadParamsQueue.Element(
            params = action.toPagingSourceLoadParams(),
            mechanism = LoadParamsQueue.Element.Mechanism.EnqueueRequest
        )
        if (action.jump) {
            appendQueue.jump(element)
        } else {
            appendQueue.addLast(element)
        }
    }

    override suspend fun enqueuePrepend(action: Action.Enqueue<K>) = mutex.withLock {
        val element = LoadParamsQueue.Element(
            params = action.toPagingSourceLoadParams(),
            mechanism = LoadParamsQueue.Element.Mechanism.EnqueueRequest
        )
        prependQueue.addLast(element)
    }

    override suspend fun addPendingJob(key: K, inFlight: Boolean) {
        pendingJobManager.addPendingJob(key, inFlight)
    }

    override suspend fun updateExistingPendingJob(key: K, inFlight: Boolean, completed: Boolean) {
        pendingJobManager.updateExistingJob(key, inFlight, completed)
    }

    override suspend fun hasPendingJobs(): Boolean {
        return pendingJobManager.hasPendingJobs()
    }

    override suspend fun clearQueues() = mutex.withLock {
        appendQueue.clear()
        prependQueue.clear()
        pendingJobManager.clearPendingJobs()
    }

    private fun Action.Enqueue<K>.toPagingSourceLoadParams(): PagingSource.LoadParams<K> {
        return PagingSource.LoadParams(
            key = key,
            strategy = strategy,
            direction = direction
        )
    }
}