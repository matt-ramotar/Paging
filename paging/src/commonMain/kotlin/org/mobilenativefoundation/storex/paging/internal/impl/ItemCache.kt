package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import org.mobilenativefoundation.store.cache5.CacheBuilder
import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.PagingConfig
import org.mobilenativefoundation.storex.paging.Quantifiable


class ItemCache<Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>> {
    private val itemMemoryCache =
        CacheBuilder<Quantifiable<Id>, V>().build()

    fun getItem(id: Quantifiable<Id>): V? {
        return itemMemoryCache.getIfPresent(id)
    }

    fun updateItem(item: V) {
        itemMemoryCache.put(item.id, item)
    }

    fun removeItem(id: Quantifiable<Id>) {
        itemMemoryCache.invalidate(id)
    }

    fun clear() {
        itemMemoryCache.invalidateAll()
    }
}

class PageCache<Id : Comparable<Id>, Q: Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>>(
    maxSize: Int,
    pagingConfig: PagingConfig<Id, K>,
    private val itemCache: ItemCache<Id,Q, V>
) {

    private val maxPages = (maxSize / pagingConfig.pageSize).toLong()

    internal data class PageNode<K : Any>(
        val key: K,
        var placeholder: Boolean = false,
        var prev: PageNode<K>? = null,
        var next: PageNode<K>? = null
    )

    private val pageMemoryCache = CacheBuilder<K, List<Quantifiable<Id>>>().maximumSize(maxPages).build()
    private val pageNodeMap = CacheBuilder<K, PageNode<K>>().maximumSize(maxPages).build()
    private var headPageRef: AtomicRef<PageNode<K>?> = atomic(null)
    private var tailPageRef: AtomicRef<PageNode<K>?> = atomic(null)

    fun getPage(key: K): List<Quantifiable<Id>>? {
        return pageMemoryCache.getIfPresent(key)
    }

    internal fun getPageNode(key: K): PageNode<K>? {
        return pageNodeMap.getIfPresent(key)
    }

    fun addPage(key: K, items: List<Quantifiable<Id>>, placeholders: Boolean = false) {
        val pageNode = PageNode(key, placeholders)
        when {
            headPageRef.value == null -> {
                headPageRef.update { pageNode }
                tailPageRef.update { pageNode }
            }

            placeholders -> {
                pageNode.next = headPageRef.value
                headPageRef.update {
                    it?.prev = pageNode
                    it
                }
                headPageRef.update { pageNode }
            }

            else -> {
                pageNode.prev = tailPageRef.value
                tailPageRef.update {
                    it?.next = pageNode
                    it
                }
                tailPageRef.update { pageNode }
            }
        }
        pageMemoryCache.put(key, items)
        pageNodeMap.put(key, pageNode)
    }

    fun removePage(key: K) {
        val pageNode = pageNodeMap.getIfPresent(key) ?: return

        if (pageNode.prev == null) {
            headPageRef.update { pageNode.next }
        } else {
            pageNode.prev?.next = pageNode.next
        }

        if (pageNode.next == null) {
            tailPageRef.update { pageNode.prev }
        } else {
            pageNode.next?.prev = pageNode.prev
        }

        pageNodeMap.invalidate(key)
        pageMemoryCache.invalidate(key)
    }

    fun getItemsInOrder(): List<V?> {
        val items = mutableListOf<V?>()
        var current = headPageRef.value
        while (current != null) {
            val pageItemIds = getPage(current.key) ?: emptyList()
            items.addAll(pageItemIds.map { itemCache.getItem(it) })
            current = current.next
        }
        return items
    }


    fun clear() {
        pageMemoryCache.invalidateAll()
        pageNodeMap.invalidateAll()
        headPageRef.update { null }
        tailPageRef.update { null }
    }
}