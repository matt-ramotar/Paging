package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.persistence.api.DataPersistence
import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingConfig
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.PlaceholderFactory
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.api.PagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LinkedHashMapManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LinkedHashMapManager.PageNode
import org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.api.ItemMemoryCache
import org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.api.PageMemoryCache

internal class RealLinkedHashMapManager<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val pageMemoryCache: PageMemoryCache<Id, K, V>,
    private val itemMemoryCache: ItemMemoryCache<Id, V>,
    private val pagingConfig: PagingConfig<Id, K>,
    private val persistence: DataPersistence<Id, K, V>?,
    private val logger: PagingLogger,
    private val placeholderFactory: PlaceholderFactory<Id, K, V>
) : LinkedHashMapManager<Id, K, V> {

    // Pushing updates from the memory cache when it's modified
    // More efficient and responsive than polling
    private val itemUpdates = MutableSharedFlow<Pair<Id, V?>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val mutex = Mutex()

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

    /**
     * This method first checks the in-memory cache, and if the item is not found,
     * it attempts to retrieve it from the persistent storage.
     */
    override suspend fun getItem(id: Id): V? = mutex.withLock {
        return getCachedItem(id) ?: getPersistedItem(id)
    }

    override suspend fun getPersistedItem(id: Id): V? {
        val result = persistence?.items?.getItem(id) ?: return null

        return when (result) {
            is PersistenceResult.Success -> result.data?.also { item ->
                // If found in persistent storage, update the memory cache
                itemMemoryCache[id] = item
            }

            is PersistenceResult.Error -> {
                logger.debug("Error retrieving item: ${result.message}")
                null
            }

            PersistenceResult.Skipped -> {
                // Do nothing
                null
            }
        }
    }

    private suspend fun getPersistedItems(predicate: (V) -> Boolean): List<V>? {
        return when (val result = persistence?.items?.queryItems(predicate)) {
            is PersistenceResult.Error -> {
                logger.debug("Error querying items: ${result.message}")
                null
            }

            null, PersistenceResult.Skipped -> null

            is PersistenceResult.Success -> result.data
        }
    }

    override suspend fun getCachedItem(id: Id): V? {
        return itemMemoryCache[id]
    }

    override suspend fun getCachedPage(key: K): PagingSource.LoadResult.Data<Id, K, V>? {
        return pageMemoryCache[key]
    }

    override suspend fun getPersistedPage(key: K): PagingSource.LoadResult.Data<Id, K, V>? {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsInOrder(): List<V?> {
        val items = mutableListOf<V?>()

        var current = headPage

        while (current != null) {
            items.addAll(getItemsFromCache(current.key))
            current = current.next
        }

        return items
    }

    private suspend fun getItemsFromCache(key: K): List<V?> {
        return pageMemoryCache[key]!!.let { data ->
            val ids = data.items.map { it.id }
            ids.map { id ->
                if (id == pagingConfig.placeholderId) {
                    null
                } else {
                    itemMemoryCache[id]
                }
            }
        }
    }


    override suspend fun putPageNode(key: K, pageNode: PageNode<K>) {
        pageNodeMap[key] = pageNode
    }

    override suspend fun saveItem(item: V) = mutex.withLock {
        // Save item to memory cache.
        if (item.id !in itemMemoryCache) {
            itemCount++
        }
        itemMemoryCache[item.id] = item

        // Emit update
        itemUpdates.emit(item.id to item)

        // Save item to database
        val params = idToKeyMap[item.id]?.let { keyToParamsMap[it] }
        saveItemToDb(item, params)
    }

    private suspend fun saveItemToDb(
        item: V,
        params: PagingSource.LoadParams<K>?
    ): PersistenceResult<Unit> {
        return persistence?.let {
            persistence.items.saveItem(item, params)
        } ?: PersistenceResult.Skipped
    }

    override suspend fun appendPage(
        params: PagingSource.LoadParams<K>,
        loadResult: PagingSource.LoadResult.Data<Id, K, V>
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

        pageMemoryCache[params.key] = loadResult
        pageNodeMap[params.key] = pageNode
        pageCount++

        saveNormalizedPageToDb(params, loadResult)

        trimToMaxSize()
    }

    override suspend fun prependPage(
        params: PagingSource.LoadParams<K>,
        loadResult: PagingSource.LoadResult.Data<Id, K, V>
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

        pageMemoryCache[params.key] = loadResult
        pageNodeMap[params.key] = pageNode
        pageCount++

        saveNormalizedPageToDb(params, loadResult)

        trimToMaxSize()
    }


    private suspend fun trimToMaxSize() {
        while (itemMemoryCache.size > pagingConfig.maxSize) {
            val oldestKey = itemMemoryCache.keys.firstOrNull()
            if (oldestKey != null) {
                removeItem(oldestKey)
            } else {
                break
            }
        }
    }

    override suspend fun prependPlaceholders(params: PagingSource.LoadParams<K>) {

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

        pageMemoryCache[params.key] = createPlaceholderData(params)
        pageNodeMap[params.key] = pageNode
        pageCount++
        trimToMaxSize()
    }

    override suspend fun appendPlaceholders(params: PagingSource.LoadParams<K>) {

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

        pageMemoryCache[params.key] = createPlaceholderData(params)
        pageNodeMap[params.key] = pageNode
        pageCount++
    }

    private fun createPlaceholderData(params: PagingSource.LoadParams<K>): PagingSource.LoadResult.Data<Id, K, V> {
        return PagingSource.LoadResult.Data(
            items = List(pagingConfig.pageSize) { index ->
                placeholderFactory.create(
                    index,
                    params
                )
            },
            prevKey = null, // TODO(): Get the prev key
            params = params,
            nextKey = null,
            origin = PagingSource.LoadResult.Data.Origin.Placeholder,
        )
    }

    override suspend fun removeItem(id: Id) = mutex.withLock {
        // Update pointers
        val key = idToKeyMap[id]
        if (key != null) {
            val page = pageMemoryCache[key]
            if (page != null) {
                val updatedPageItems = page.items.toMutableList()
                updatedPageItems.removeAll { it.id == id }
                if (updatedPageItems.isEmpty()) {
                    // Remove the page too, because it has no items
                    removePage(key)
                } else {
                    pageMemoryCache[key] = page.copy(items = updatedPageItems)
                }
            }
        }

        val result =
            persistence?.let { persistence.items.removeItem(id) } ?: PersistenceResult.Skipped
        itemMemoryCache.remove(id)

        // Emit update
        itemUpdates.emit(id to null)

        itemCount--
        result
    }


    override suspend fun removePage(key: K) {
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
            val page = pageMemoryCache[node.key]
            val pageItemIds = page?.items?.map { it.id }
            pageItemIds?.forEach { id -> removeItem(id) }

            // Remove from memory cache
            pageMemoryCache.remove(node.key)

            // Remove from database
            persistence?.let { persistence.pages.removePage(keyToParamsMap[node.key]!!) }
        }
    }

    private suspend fun saveNormalizedPageToDb(
        params: PagingSource.LoadParams<K>,
        loadResult: PagingSource.LoadResult.Data<Id, K, V>
    ) {
        persistence?.let { persistence.pages.savePage(params, loadResult) }
    }

    override suspend fun isInFlight(key: K): Boolean {
        return pageNodeMap[key]?.isInFlight == true
    }

    override suspend fun isCached(key: K): Boolean {
        val page = pageMemoryCache[key]
        return page != null
    }

    override suspend fun isInDatabase(params: PagingSource.LoadParams<K>): Boolean {
        var isInDatabase = false

        persistence?.let {
            isInDatabase = persistence.pages.exists(params) is PersistenceResult.Success
        }

        return isInDatabase
    }

    override suspend fun removeAllItems(): PersistenceResult<Unit> = mutex.withLock {
        // Clear memory cache
        idToKeyMap.clear()
        itemMemoryCache.clear()

        // Reset size counters
        itemCount = 0

        // Clear persistence, if not null
        persistence?.let {
            persistence.items.clearAllItems()
        } ?: PersistenceResult.Skipped
    }

    override suspend fun removeAllPages(): PersistenceResult<Unit> = mutex.withLock {
        // Clear memory cache
        keyToParamsMap.clear()
        pageNodeMap.clear()
        pageMemoryCache.clear()

        // Reset pointers
        headPage = null
        tailPage = null

        // Reset size counters
        pageCount = 0

        // Clear persistence, if not null
        persistence?.let {
            persistence.pages.clearAllPages()
        } ?: PersistenceResult.Skipped
    }

    override suspend fun invalidate() {
        removeAllItems()
        removeAllPages()
    }

    override suspend fun queryItems(predicate: (V) -> Boolean): PersistenceResult<List<V>> =
        mutex.withLock {
            val cachedItems = itemMemoryCache.values.filter(predicate)
            val persistedItems = getPersistedItems(predicate) ?: emptyList()

            val combinedItems = (cachedItems + persistedItems).distinctBy { it.id }

            PersistenceResult.Success(combinedItems)
        }

    /**
     * Provides a flow of updates for a specific item.
     *
     * This method combines updates from both the memory cache and persistent storage.
     *
     * @param id The identifier of the item to observe.
     * @return A Flow emitting the latest state of the item, or null if the item is deleted.
     */
    override fun observeItem(id: Id): Flow<V?> = flow {
        // Emit the initial value
        emit(getItem(id))

        val memoryUpdates = itemUpdates
            .filter { it.first == id }  // Filter updates for this specific id
            .map { it.second }

        val persistenceUpdates = persistence?.items?.observeItem(id) ?: emptyFlow()

        memoryUpdates.combine(persistenceUpdates) { memoryItem, persistedItem ->
            mutex.withLock {
                when {
                    memoryItem != null -> {
                        if (persistedItem != null && persistedItem != memoryItem) {
                            // If persistence has a different value, update it
                            val params = idToKeyMap[id]?.let { keyToParamsMap[it] }

                            if (params != null) {
                                persistence?.items?.saveItem(memoryItem, params)
                            }
                        }
                        memoryItem
                    }

                    persistedItem != null -> {
                        // Update memory cache with persisted item
                        itemMemoryCache[id] = persistedItem
                        persistedItem
                    }

                    else -> null
                }
            }
        }.collect { emit(it) }
    }.distinctUntilChanged()
}