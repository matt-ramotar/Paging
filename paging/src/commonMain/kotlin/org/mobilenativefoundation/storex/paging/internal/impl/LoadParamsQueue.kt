package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.PagingSource


data class LoadParamsQueueElement<K: Any>(
    val params: PagingSource.LoadParams<K>,
    val mechanism: Mechanism
) {
    enum class Mechanism {
        EnqueueRequest,
        NetworkLoadResponse,
        InitialLoad
    }
}

class LoadParamsQueue<K : Any> {
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

    fun clear() = queue.clear()

    fun isNotEmpty(): Boolean = queue.isNotEmpty()
}