package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.PagingSource


class LoadParamsQueue<K : Any> {
    private val queue: ArrayDeque<PagingSource.LoadParams<K>> = ArrayDeque()
    private val processed = linkedSetOf<PagingSource.LoadParams<K>>()

    fun addLast(params: PagingSource.LoadParams<K>) {
        if (processed.contains(params)) return
        queue.addLast(params)
        processed.add(params)
    }

    fun first(): PagingSource.LoadParams<K> = queue.first()
    fun removeFirst(): PagingSource.LoadParams<K> = queue.removeFirst()
    fun last(): PagingSource.LoadParams<K> = queue.last()
    fun removeLast(): PagingSource.LoadParams<K> = queue.removeLast()

    fun clear() = queue.clear()

    fun isNotEmpty(): Boolean = queue.isNotEmpty()
}