package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.Comparator
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadParamsQueue


/**
 * Implementation of LoadParamsQueue that manages load parameters for paging operations.
 *
 * @param PageRequestKey The type of the paging key, which must be Comparable.
 */
internal class RealLoadParamsQueue<PageRequestKey : Any>(
    private val pageRequestKeyComparator: Comparator<PageRequestKey>
) : LoadParamsQueue<PageRequestKey> {

    // Queue is ordered from the lowest key (first) to the highest key (last)
    private val queue: ArrayDeque<LoadParamsQueue.Element<PageRequestKey>> = ArrayDeque()

    // Set to keep track of processed elements to avoid duplicates
    private val processed = linkedSetOf<LoadParamsQueue.Element<PageRequestKey>>()

    private val size = atomic(0)
    private val mutex = Mutex()

    override suspend fun addLast(element: LoadParamsQueue.Element<PageRequestKey>) = mutex.withLock {
        if (element !in processed) {
            queue.addFirst(element)
            processed.add(element)
            size.incrementAndGet()
        }
    }

    override suspend fun addFirst(element: LoadParamsQueue.Element<PageRequestKey>) {
        if (processed.contains(element)) return
        queue.addFirst(element)
        processed.add(element)
    }

    override suspend fun first(): LoadParamsQueue.Element<PageRequestKey> = mutex.withLock {
        queue.firstOrNull() ?: throw NoSuchElementException("Queue is empty")
    }

    override suspend fun removeFirst(): LoadParamsQueue.Element<PageRequestKey> = mutex.withLock {
        size.decrementAndGet()
        queue.removeFirst()
    }

    override suspend fun removeLast(predicate: (element: LoadParamsQueue.Element<PageRequestKey>) -> Boolean) {
        mutex.withLock {
            val index = queue.indexOfLast(predicate)
            queue.removeAt(index)
        }
    }

    override suspend fun removeFirst(predicate: (element: LoadParamsQueue.Element<PageRequestKey>) -> Boolean) {
        mutex.withLock {
            val index = queue.indexOfFirst(predicate)
            queue.removeAt(index)
        }
    }

    override suspend fun last(): LoadParamsQueue.Element<PageRequestKey> = mutex.withLock {
        queue.lastOrNull() ?: throw NoSuchElementException("Queue is empty")
    }

    override suspend fun removeLast(): LoadParamsQueue.Element<PageRequestKey> = mutex.withLock {
        size.decrementAndGet()
        queue.removeLast()
    }

    override suspend fun jump(element: LoadParamsQueue.Element<PageRequestKey>) {
        mutex.withLock {
            // Remove all elements with keys less than or equal to the jump element

            val elementsToRemove = queue.filter {
                // We use <= to ensure we also remove any existing element with the same key
                // This also allows us to update the mechanism if needed
                pageRequestKeyComparator.compare(it.params.key, element.params.key) <= 0
            }

            // Remove these elements from the queue
            // We do not need to also update the set of processed elements
            // If we are skipping load parameters in the queue, they shouldn't be considered processed
            queue.removeAll(elementsToRemove)

            // Update the size of the queue
            size.addAndGet(-elementsToRemove.size)


            if (element !in processed) {
                queue.addFirst(element)
                processed.add(element)

            }

            // Add the jump element to the front of the queue
            addFirst(element)
            // And update the size of the queue
            size.incrementAndGet()
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            queue.clear()
            processed.clear()
            size.getAndSet(0)
        }
    }

    override suspend fun isNotEmpty(): Boolean = mutex.withLock {
        size.value > 0
    }

    override suspend fun size(): Int = mutex.withLock {
        size.value
    }
}