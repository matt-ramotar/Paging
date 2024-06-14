package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.storex.paging.*


typealias PageMemoryCache<K, Q> = MutableMap<K, List<Q>>
typealias ItemMemoryCache<Q, V> = MutableMap<Q, V>

@OptIn(InternalSerializationApi::class)
class PagingLinkedHashMap<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    private val pageMemoryCache: PageMemoryCache<K, Q>,
    private val itemMemoryCache: ItemMemoryCache<Q, V>,
    private val registry: KClassRegistry<Id, Q, K, V, E>,
    private val pagingConfig: PagingConfig<Id, Q, K>,
    private val db: PagingDb?,
) {
    private var headPage: PageNode<K>? = null
    private var tailPage: PageNode<K>? = null

    private var pageCount: Int = 0
    private var itemCount: Int = 0

    private val pageNodeMap = mutableMapOf<K, PageNode<K>>()

    private val keyToParamsMap = mutableMapOf<K, PagingSource.LoadParams<K>>()
    private val idToKeyMap = mutableMapOf<Q, K>()

    fun getPageNode(key: K): PageNode<K>? {
        return pageNodeMap[key]
    }


    fun getItem(id: Q): V? {
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

    fun saveItem(item: V, encodedParams: String?) {
        // Save item to memory cache.
        if (item.id !in itemMemoryCache) {
            itemCount++
        }
        itemMemoryCache[item.id] = item

        // Save item to database.
        saveItemToDb(item, encodedParams)
    }

    private fun saveItemToDb(item: V, encodedParams: String?) {
        db?.let {
            val id = Json.encodeToString(registry.q.serializer(), item.id)

            if (encodedParams != null) {

                db.itemQueries.setItem(
                    Item(
                        id,
                        Json.encodeToString(registry.value.serializer(), item),
                        encodedParams
                    )
                )
            } else {
                db.itemQueries.updateItem(
                    Json.encodeToString(registry.value.serializer(), item),
                    id
                )
            }
        }
    }

    fun appendPage(
        params: PagingSource.LoadParams<K>,
        encodedParams: String,
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, Q, K, V, E>>
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
            db.itemQueries
        }

        saveNormalizedPageToDb(encodedParams, fetcherResult)

        trimToMaxSize()
    }

    fun prependPage(
        params: PagingSource.LoadParams<K>,
        encodedParams: String,
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, Q, K, V, E>>
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

        saveNormalizedPageToDb(encodedParams, fetcherResult)

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

    fun removeItem(id: Q) {
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

        db?.let {
            val encodedItemId = Json.encodeToString(registry.q.serializer(), id)
            db.itemQueries.removeItem(encodedItemId)
            itemMemoryCache.remove(id)
            itemCount--
        }
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
            db?.let {
                val encodedParams = Json.encodeToString(
                    PagingSource.LoadParams.serializer(registry.key.serializer()),
                    keyToParamsMap[node.key]!!
                )

                db.pageQueries.removePage(encodedParams)
            }
        }
    }

    private fun saveNormalizedPageToDb(
        encodedParams: String,
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, Q, K, V, E>>
    ) {
        db?.let {
            val page = Page(
                params = encodedParams,
                nextKey = fetcherResult.value.nextKey?.let { key ->
                    Json.encodeToString(registry.key.serializer(), key)
                },
                prevKey = fetcherResult.value.prevKey?.let { key ->
                    Json.encodeToString(registry.key.serializer(), key)
                },
                extras = fetcherResult.value.extras?.toString()
            )

            db.pageQueries.setPage(page)
        }
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
            val encodedParams = Json.encodeToString(
                PagingSource.LoadParams.serializer(registry.key.serializer()),
                params
            )

            val encodedPage = db.pageQueries.getPage(encodedParams).executeAsOneOrNull()
            isInDatabase = encodedPage != null
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
