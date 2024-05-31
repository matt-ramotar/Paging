package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.PagingSource


data class LoadParamsQueueElement<K : Any>(
    val params: PagingSource.LoadParams<K>,
    val mechanism: Mechanism
) {
    enum class Mechanism {
        EnqueueRequest,
        NetworkLoadResponse,
        InitialLoad
    }
}

class LoadParamsQueue<K : Comparable<K>> {

    // first should have lowest key
    // last should have highest key
    private val queue: ArrayDeque<LoadParamsQueueElement<K>> = ArrayDeque()
    private val processed = linkedSetOf<LoadParamsQueueElement<K>>()

    fun addLast(params: LoadParamsQueueElement<K>) {
        if (processed.contains(params)) return
        queue.addLast(params)
        processed.add(params)
    }

    fun addFirst(params: LoadParamsQueueElement<K>) {
        if (processed.contains(params)) return
        queue.addFirst(params)
        processed.add(params)
    }

    fun first(): LoadParamsQueueElement<K> = queue.first()
    fun removeFirst(): LoadParamsQueueElement<K> = queue.removeFirst()
    fun last(): LoadParamsQueueElement<K> = queue.last()
    fun removeLast(): LoadParamsQueueElement<K> = queue.removeLast()

    fun jump(params: LoadParamsQueueElement<K>) {
        println("QUEUE SIZE = ${queue.size}")

        queue.removeAll {
            // it's possible that the element is already in the queue
            // we do <= so that we don't need to also find and update the already enqueued element's mechanism
            // we need to update mechanism somehow so that real pager will load it bypassing fetching strategy
            it.params.key <= params.params.key
        }
        println("REMOVED ALL")
        println("QUEUE SIZE = ${queue.size}")

        addFirst(params)

        println("QUEUE SIZE = ${queue.size}")
    }

    fun clear() = queue.clear()

    fun isNotEmpty(): Boolean = queue.isNotEmpty()
}