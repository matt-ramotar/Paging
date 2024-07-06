package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.PagingSource

/**
 * Interface for a queue that manages load parameters for paging operations.
 *
 * @param K The type of the paging key, which must be Comparable.
 */
internal interface LoadParamsQueue<K : Comparable<K>> {
    /**
     * Represents an element in the queue.
     *
     * @property params The load parameters for a paging operation.
     * @property mechanism The mechanism that triggered this load operation.
     */
    data class Element<K : Any>(
        val params: PagingSource.LoadParams<K>,
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
    fun addLast(element: Element<K>)

    /**
     * Adds an element to the beginning of the queue if it hasn't been processed before.
     *
     * @param element The element to add.
     */
    fun addFirst(element: Element<K>)

    /**
     * Returns the first element in the queue without removing it.
     *
     * @return The first element in the queue.
     */
    fun first(): Element<K>

    /**
     * Removes and returns the first element in the queue.
     *
     * @return The first element in the queue.
     */
    fun removeFirst(): Element<K>

    /**
     * Returns the last element in the queue without removing it.
     *
     * @return The last element in the queue.
     */
    fun last(): Element<K>

    /**
     * Removes and returns the last element in the queue.
     *
     * @return The last element in the queue.
     */
    fun removeLast(): Element<K>

    /**
     * Jumps to a specific element in the queue, removing all elements with keys less than or equal to it.
     *
     * @param element The element to jump to.
     */
    fun jump(element: Element<K>)

    /**
     * Clears all elements from the queue.
     */
    fun clear()

    /**
     * Checks if the queue is not empty.
     *
     * @return true if the queue is not empty, false otherwise.
     */
    fun isNotEmpty(): Boolean
}
