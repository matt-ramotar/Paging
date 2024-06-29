package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.PagingSource


class LoadParamsQueue<K : Comparable<K>> {

    data class Element<K : Any>(
        val params: PagingSource.LoadParams<K>,
        val mechanism: Mechanism
    ) {
        enum class Mechanism {
            EnqueueRequest,
            NetworkLoadResponse,
            InitialLoad
        }
    }

    // first should have lowest key
    // last should have highest key
    private val queue: ArrayDeque<Element<K>> = ArrayDeque()
    private val processed = linkedSetOf<Element<K>>()

    fun addLast(element: Element<K>) {
        if (processed.contains(element)) return
        queue.addLast(element)
        processed.add(element)
    }

    fun addFirst(element: Element<K>) {
        if (processed.contains(element)) return
        queue.addFirst(element)
        processed.add(element)
    }

    fun first(): Element<K> = queue.first()
    fun removeFirst(): Element<K> = queue.removeFirst()
    fun last(): Element<K> = queue.last()
    fun removeLast(): Element<K> = queue.removeLast()

    fun jump(element: Element<K>) {
        println("QUEUE SIZE = ${queue.size}")

        queue.removeAll {
            // it's possible that the element is already in the queue
            // we do <= so that we don't need to also find and update the already enqueued element's mechanism
            // we need to update mechanism somehow so that real pager will load it bypassing fetching strategy
            it.params.key <= element.params.key
        }
        println("REMOVED ALL")
        println("QUEUE SIZE = ${queue.size}")

        addFirst(element)

        println("QUEUE SIZE = ${queue.size}")
    }

    fun clear() = queue.clear()

    fun isNotEmpty(): Boolean = queue.isNotEmpty()
}