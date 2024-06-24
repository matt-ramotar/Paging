package org.mobilenativefoundation.storex.paging.internal.impl.store

import kotlinx.serialization.InternalSerializationApi
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.PagingConfig
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.scope.Database


typealias PageMemoryCache<K, Id> = MutableMap<K, List<Id>>
typealias ItemMemoryCache<Q, V> = MutableMap<Q, V>

@OptIn(InternalSerializationApi::class)
class LinkedHashMapManager<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val pageMemoryCache: PageMemoryCache<K, Id>,
    private val itemMemoryCache: ItemMemoryCache<Id, V>,
    private val pagingConfig: PagingConfig<Id, K>,
    private val db: Database<Id, K, V>?,
) {
    private var headPage: PageNode<K>? = null
    private var tailPage: PageNode<K>? = null

    private var pageCount: Int = 0
    private var itemCount: Int = 0

    private val pageNodeMap = mutableMapOf<K, PageNode<K>>()

    private val keyToParamsMap = mutableMapOf<K, PagingSource.LoadParams<K>>()
    private val idToKeyMap = mutableMapOf<Id, K>()

    fun getPageNode(key: K): PageNode<K>? {
        return pageNodeMap[key]
    }


    fun getItem(id: Id): V? {
        return itemMemoryCache[id]
    }

    fun getItemsInOrder(): List<V?> {
        val items = mutableListOf<V?>()

        var current = headPage

        while (current != null) {
            items.addAll(getItemsFromCache(current.key))
            current = current.next
        }

        return items
    }

    private fun getItemsFromCache(key: K): List<V?> {
        return pageMemoryCache[key]!!.let { ids ->
            ids.map { id ->
                if (id == pagingConfig.placeholderId) {
                    null
                } else {
                    itemMemoryCache[id]
                }
            }
        }
    }

    fun putPageNode(key: K, pageNode: PageNode<K>) {
        pageNodeMap[key] = pageNode
    }

    fun saveItem(item: V, params: PagingSource.LoadParams<K>?) {
        // Save item to memory cache.
        if (item.id !in itemMemoryCache) {
            itemCount++
        }
        itemMemoryCache[item.id] = item

        // Save item to database.
        saveItemToDb(item, params)
    }

    private fun saveItemToDb(item: V, params: PagingSource.LoadParams<K>?) {
        db?.let {

            if (params != null) {
                db.itemQueries.setItem(item.id, item, params)
            } else {
                db.itemQueries.updateItem(item.id, item)
            }
        }
    }

    fun appendPage(
        params: PagingSource.LoadParams<K>,
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, K, V>>
    ) {
        val pageNode = if (params.key !in pageNodeMap) {
            val node = PageNode(key = params.key)

            if (tailPage == null) {
                headPage = node
                tailPage = node
            } else {
                tailPage?.next = node
                node.prev = tailPage
                tailPage = node
            }

            node
        } else {
            val node = pageNodeMap[params.key]!!
            node.copy(isPlaceholder = false)
        }

        pageMemoryCache[params.key] = fetcherResult.value.items.map { it.id }
        pageNodeMap[params.key] = pageNode
        pageCount++

        db?.let {
            // TODO()
            db.itemQueries
        }

        saveNormalizedPageToDb(params, fetcherResult)

        trimToMaxSize()
    }

    fun prependPage(
        params: PagingSource.LoadParams<K>,
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, K, V>>
    ) {
        val pageNode = if (params.key !in pageNodeMap) {
            val node = PageNode(key = params.key)

            if (headPage == null) {
                headPage = node
                tailPage = node
            } else {
                headPage?.prev = node
                node.next = headPage
                headPage = node
            }

            node
        } else {
            val node = pageNodeMap[params.key]!!
            node.copy(isPlaceholder = false)
        }

        pageMemoryCache[params.key] = fetcherResult.value.items.map { it.id }
        pageNodeMap[params.key] = pageNode
        pageCount++

        saveNormalizedPageToDb(params, fetcherResult)

        trimToMaxSize()
    }


    private fun trimToMaxSize() {
        while (itemMemoryCache.size > pagingConfig.maxSize) {
            val oldestKey = itemMemoryCache.keys.firstOrNull()
            if (oldestKey != null) {
                removeItem(oldestKey)
            } else {
                break
            }
        }
    }

    fun prependPlaceholders(params: PagingSource.LoadParams<K>) {

        if (pagingConfig.placeholderId == null) {
            return
        }

        val pageNode = PageNode(key = params.key, isPlaceholder = true)

        if (headPage == null) {
            headPage = pageNode
            tailPage = pageNode
        } else {
            headPage?.prev = pageNode
            pageNode.next = headPage
            headPage = pageNode
        }

        pageMemoryCache[params.key] = List(pagingConfig.pageSize) { pagingConfig.placeholderId }
        pageNodeMap[params.key] = pageNode
        pageCount++
        trimToMaxSize()
    }

    fun appendPlaceholders(params: PagingSource.LoadParams<K>) {

        if (pagingConfig.placeholderId == null) {
            return
        }

        val pageNode = PageNode(key = params.key, isPlaceholder = true)

        if (tailPage == null) {
            headPage = pageNode
            tailPage = pageNode
        } else {
            tailPage?.next = pageNode
            pageNode.prev = tailPage
            tailPage = pageNode
        }

        pageMemoryCache[params.key] = List(pagingConfig.pageSize) { pagingConfig.placeholderId }
        pageNodeMap[params.key] = pageNode
        pageCount++
    }

    fun removeItem(id: Id) {
        // Update pointers
        val key = idToKeyMap[id]
        if (key != null) {
            val pageItemIds = pageMemoryCache[key]
            if (pageItemIds != null) {
                val updatedPageItemIds = pageItemIds.toMutableList()
                updatedPageItemIds.remove(id)
                if (updatedPageItemIds.isEmpty()) {
                    // Remove the page too, because it has no items
                    removePage(key)
                } else {
                    pageMemoryCache[key] = updatedPageItemIds
                }
            }
        }

        db?.let { db.itemQueries.removeItem(id) }
        itemMemoryCache.remove(id)
        itemCount--
    }


    fun removePage(key: K) {
        pageNodeMap[key]?.let { node ->
            // Update pointers
            if (node.prev == null) {
                headPage = node.next
            } else {
                node.prev?.next = node.next
            }

            if (node.next == null) {
                tailPage = node.prev
            } else {
                node.next?.prev = node.prev
            }

            // Remove reference
            pageNodeMap.remove(node.key)

            // Remove page's items
            val pageItemIds = pageMemoryCache[node.key]
            pageItemIds?.forEach { id -> removeItem(id) }

            // Remove from memory cache
            pageMemoryCache.remove(node.key)

            // Remove from database
            db?.let { db.pageQueries.removePage(keyToParamsMap[node.key]!!) }
        }
    }

    private fun saveNormalizedPageToDb(
        params: PagingSource.LoadParams<K>,
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, K, V>>
    ) {
        db?.let { db.pageQueries.setPage(params, fetcherResult.value) }
    }

    fun isInFlight(key: K): Boolean {
        return pageNodeMap[key]?.isInFlight == true
    }

    fun isCached(key: K): Boolean {
        val page = pageMemoryCache[key]
        return !page.isNullOrEmpty()
    }

    fun isInDatabase(params: PagingSource.LoadParams<K>): Boolean {
        var isInDatabase = false

        db?.let {
            isInDatabase = db.pageQueries.exists(params)
        }

        return isInDatabase
    }

    fun invalidate() {
        // Clear memory cache
        idToKeyMap.clear()
        keyToParamsMap.clear()
        pageNodeMap.clear()
        pageMemoryCache.clear()
        itemMemoryCache.clear()

        // Reset pointers
        headPage = null
        tailPage = null

        // Reset size counters
        itemCount = 0
        pageCount = 0

        // Clear the database, if applicable
        db?.let {
            db.itemQueries.removeAllItems()
            db.pageQueries.removeAllPages()
        }
    }

    data class PageNode<K : Any>(
        val key: K,
        var isPlaceholder: Boolean = false,
        var isInFlight: Boolean = false,
        var prev: PageNode<K>? = null,
        var next: PageNode<K>? = null
    )

}