package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadParamsQueue


/**
 * Implementation of LoadParamsQueue that manages load parameters for paging operations.
 *
 * @param K The type of the paging key, which must be Comparable.
 */
class RealLoadParamsQueue<K : Comparable<K>> : LoadParamsQueue<K> {

    // Queue is ordered from lowest key (first) to highest key (last)
    private val queue: ArrayDeque<LoadParamsQueue.Element<K>> = ArrayDeque()

    // Set to keep track of processed elements to avoid duplicates
    private val processed = linkedSetOf<LoadParamsQueue.Element<K>>()

    override fun addLast(element: LoadParamsQueue.Element<K>) {
        if (processed.contains(element)) return
        queue.addLast(element)
        processed.add(element)
    }

    override fun addFirst(element: LoadParamsQueue.Element<K>) {
        if (processed.contains(element)) return
        queue.addFirst(element)
        processed.add(element)
    }

    override fun first(): LoadParamsQueue.Element<K> = queue.first()

    override fun removeFirst(): LoadParamsQueue.Element<K> = queue.removeFirst()

    override fun last(): LoadParamsQueue.Element<K> = queue.last()

    override fun removeLast(): LoadParamsQueue.Element<K> = queue.removeLast()

    override fun jump(element: LoadParamsQueue.Element<K>) {
        // Remove all elements with keys less than or equal to the jump element
        queue.removeAll {
            // We use <= to ensure we also remove any existing element with the same key
            // This also allows us to update the mechanism if needed
            it.params.key <= element.params.key
        }

        // Add the jump element to the front of the queue
        addFirst(element)
    }

    override fun clear() {
        queue.clear()
        processed.clear()
    }

    override fun isNotEmpty(): Boolean = queue.isNotEmpty()
}