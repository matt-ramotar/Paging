package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.impl.extensions.get
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.custom.TransformationStrategy
import org.mobilenativefoundation.storex.paging.internal.api.MutablePagingBuffer

class RealMutablePagingBuffer<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any, P : Any>(
    private val maxSize: Int = 500,
    private val pageStore: Store<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>>,
    private val itemStore: Store<Id, V>,
    private val transformationParams: P,
    private val transformations: List<TransformationStrategy<Id, V, P>>,
) : MutablePagingBuffer<Id, K, V, E> {

    private val pageMap = mutableMapOf<K, Node<Id>>()
    private val itemMap = mutableMapOf<Id, Node<Id>>()
    private var head: Node<Id>? = null
    private var tail: Node<Id>? = null
    private var size = 0

    override fun append(
        params: PagingSource.LoadParams<K>,
        page: PagingSource.LoadResult.Data<Id, K, V, E>
    ) {
        page.items.forEach { item ->
            val node = Node(item.id)
            if (tail == null) {
                head = node
                tail = node
            } else {
                tail?.next = node
                node.prev = tail
                tail = node
            }
            itemMap[item.id] = node
            size++
        }
        pageMap[params.key] = tail!!
        trimToMaxSize()
    }

    override fun prepend(
        params: PagingSource.LoadParams<K>,
        page: PagingSource.LoadResult.Data<Id, K, V, E>
    ) {
        page.items.asReversed().forEach { item ->
            val node = Node(item.id)
            if (head == null) {
                head = node
                tail = node
            } else {
                head?.prev = node
                node.next = head
                head = node
            }
            itemMap[item.id] = node
            size++
        }
        pageMap[params.key] = head!!
        trimToMaxSize()
    }

    override fun remove(params: PagingSource.LoadParams<K>) {
        pageMap.remove(params.key)?.let { node ->
            removeNode(node)
        }
    }

    override suspend fun get(params: PagingSource.LoadParams<K>): PagingSource.LoadResult.Normalized<Id, K, V, E> {
        return pageStore.get(params).normalized
    }


    private fun trimToMaxSize() {
        while (size > maxSize) {
            head?.let { removeNode(it) }
        }
    }

    private fun removeNode(node: Node<Id>) {
        if (node.prev == null) {
            head = node.next
        } else {
            node.prev?.next = node.next
        }
        if (node.next == null) {
            tail = node.prev
        } else {
            node.next?.prev = node.prev
        }
        itemMap.remove(node.id)
        size--
    }

    private data class Node<Id>(
        val id: Id,
        var prev: Node<Id>? = null,
        var next: Node<Id>? = null
    )

    override suspend fun get(id: Id): V? {
        return itemStore.getOrNull(id)
    }

    override suspend fun snapshot(ids: List<Id>): ItemSnapshotList<Id, V> {
        val items = ids.mapNotNull { get(it) }

        var transformed = ItemSnapshotList(items)

        transformations.forEach { transformation ->
            transformed = transformation(transformed, transformationParams)
        }

        return transformed
    }
}