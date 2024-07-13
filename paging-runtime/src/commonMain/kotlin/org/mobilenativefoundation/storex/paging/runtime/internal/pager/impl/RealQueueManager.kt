package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.Action
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadParamsQueue
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PendingJobManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.QueueManager

/**
 * Implementation of QueueManager that manages load parameter queues and pending jobs.
 *
 * @param K The type of the paging key, which must be Comparable.
 */
internal class RealQueueManager<K : Comparable<K>>(
    private val logger: PagingLogger
) : QueueManager<K> {
    override val appendQueue: LoadParamsQueue<K> = RealLoadParamsQueue()
    override val prependQueue: LoadParamsQueue<K> = RealLoadParamsQueue()

    private val pendingJobManager: PendingJobManager<K> = MutexProtectedPendingJobManager()
    private val mutex = Mutex()

    override suspend fun enqueueAppend(action: Action.Enqueue<K>) = mutex.withLock {
        logger.debug("Enqueuing append action: $action")
        val element = LoadParamsQueue.Element(
            params = action.toPagingSourceLoadParams(),
            mechanism = LoadParamsQueue.Element.Mechanism.EnqueueRequest
        )
        if (action.jump) {
            logger.debug("Jumping to element in append queue: $element")
            appendQueue.jump(element)
        } else {
            logger.debug("Adding element to end of append queue: $element")
            appendQueue.addLast(element)
        }
        logger.debug("Append queue size after enqueue: ${appendQueue.size()}")
    }

    override suspend fun enqueuePrepend(action: Action.Enqueue<K>) = mutex.withLock {
        logger.debug("Enqueuing prepend action: $action")
        val element = LoadParamsQueue.Element(
            params = action.toPagingSourceLoadParams(),
            mechanism = LoadParamsQueue.Element.Mechanism.EnqueueRequest
        )
        logger.debug("Adding element to end of prepend queue: $element")
        prependQueue.addLast(element)
        logger.debug("Prepend queue size after enqueue: ${prependQueue.size()}")
    }

    override suspend fun addPendingJob(key: K, inFlight: Boolean) {
        logger.debug("Adding pending job: key=$key, inFlight=$inFlight")
        pendingJobManager.addPendingJob(key, inFlight)
        logger.debug("Pending jobs count after addition: ${pendingJobManager.count()}")
    }

    override suspend fun updateExistingPendingJob(key: K, inFlight: Boolean, completed: Boolean) {
        logger.debug("Updating existing pending job: key=$key, inFlight=$inFlight, completed=$completed")
        pendingJobManager.updateExistingJob(key, inFlight, completed)
        logger.debug("Pending jobs count after update: ${pendingJobManager.count()}")
    }

    override suspend fun hasPendingJobs(): Boolean {
        val hasPendingJobs = pendingJobManager.hasPendingJobs()
        logger.debug("Checking for pending jobs. Has pending jobs: $hasPendingJobs")
        return hasPendingJobs
    }

    override suspend fun clearQueues() = mutex.withLock {
        logger.debug("Clearing all queues")
        appendQueue.clear()
        prependQueue.clear()
        pendingJobManager.clearPendingJobs()
        logger.debug(
            """
                All queues cleared
                Append queue size: ${appendQueue.size()}
                Prepend queue size: ${prependQueue.size()}
                Pending jobs: ${pendingJobManager.count()}
            """.trimIndent()
        )
    }

    private fun Action.Enqueue<K>.toPagingSourceLoadParams(): PagingSource.LoadParams<K> {
        logger.debug("Converting Enqueue action to PagingSource.LoadParams: $this")
        return PagingSource.LoadParams(
            key = key,
            strategy = strategy,
            direction = direction
        )
    }
}