package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.PagingSource

/**
 * Interface for a queue that manages load parameters for paging operations.
 *
 * @param PageRequestKey The type of the paging key, which must be Comparable.
 */
internal interface LoadParamsQueue<PageRequestKey: Any> {
    /**
     * Represents an element in the queue.
     *
     * @property params The load parameters for a paging operation.
     * @property mechanism The mechanism that triggered this load operation.
     */
    data class Element<PageRequestKey : Any>(
        val params: PagingSource.LoadParams<PageRequestKey>,
        val mechanism: Mechanism
    ) {
        /**
         * Enum representing the different mechanisms that can trigger a load operation.
         */
        enum class Mechanism {
            EnqueueRequest,
            NetworkLoadResponse,
            InitialLoad
        }
    }

    /**
     * Adds an element to the end of the queue if it hasn't been processed before.
     *
     * @param element The element to add.
     */
    suspend fun addLast(element: Element<PageRequestKey>)

    /**
     * Adds an element to the beginning of the queue if it hasn't been processed before.
     *
     * @param element The element to add.
     */
    suspend fun addFirst(element: Element<PageRequestKey>)

    /**
     * Returns the first element in the queue without removing it.
     *
     * @return The first element in the queue.
     */
    suspend fun first(): Element<PageRequestKey>

    /**
     * Removes and returns the first element in the queue.
     *
     * @return The first element in the queue.
     */
    suspend fun removeFirst(): Element<PageRequestKey>

    /**
     * Removes the first matching element in the queue.
     * @return The first matching element in the queue.
     */
    suspend fun removeFirst(predicate: (element: Element<PageRequestKey>) -> Boolean)

    /**
     * Removes the last matching element in the queue.
     * @return The last matching element in the queue.
     */
    suspend fun removeLast(predicate: (element: Element<PageRequestKey>) -> Boolean)

    /**
     * Returns the last element in the queue without removing it.
     *
     * @return The last element in the queue.
     */
    suspend fun last(): Element<PageRequestKey>

    /**
     * Removes and returns the last element in the queue.
     *
     * @return The last element in the queue.
     */
    suspend fun removeLast(): Element<PageRequestKey>

    /**
     * Jumps to a specific element in the queue, removing all elements with keys less than or equal to it.
     *
     * @param element The element to jump to.
     */
    suspend fun jump(element: Element<PageRequestKey>)

    /**
     * Clears all elements from the queue.
     */
    suspend fun clear()

    /**
     * Checks if the queue is not empty.
     *
     * @return true if the queue is not empty, false otherwise.
     */
    suspend fun isNotEmpty(): Boolean

    /**
     * Returns the current size of the queue.
     *
     * @return The number of elements in the queue.
     */
    suspend fun size(): Int
}
