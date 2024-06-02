package org.mobilenativefoundation.storex.paging.internal.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.db.DriverFactory
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.api.NormalizedStore


@Suppress("UNCHECKED_CAST")
@OptIn(InternalSerializationApi::class)
class RealNormalizedStore<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    private val pageFetcher: Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, Q, K, V, E>>,
    private val registry: KClassRegistry<Id, Q, K, V, E>,
    private val errorFactory: ErrorFactory<E>,
    private val itemFetcher: Fetcher<Id, V>?,
    driverFactory: DriverFactory?,
    private val maxSize: Int = 500,
    private val fetchingStateHolder: FetchingStateHolder<Id, K>,
    private val sideEffects: List<SideEffect<Id, Q, V>>,
    private val pagingConfig: PagingConfig<Id, K>
) : NormalizedStore<Id, Q, K, V, E> {

    private val db = driverFactory?.let { PagingDb(driverFactory.createDriver()) }

    private data class PageNode<K : Any>(
        val key: K,
        val placeholder: Boolean = false,
        var prev: PageNode<K>? = null,
        var next: PageNode<K>? = null
    )

    private val idToKeyMap = mutableMapOf<Quantifiable<Id>, K>()
    private val keyToParamsMap = mutableMapOf<K, PagingSource.LoadParams<K>>()

    private var headPage: PageNode<K>? = null
    private var tailPage: PageNode<K>? = null

    private val pageNodeMap = mutableMapOf<K, PageNode<K>>()

    private var pageMemoryCache = mutableMapOf<K, List<Quantifiable<Id>>>()
    private var itemMemoryCache = mutableMapOf<Quantifiable<Id>, V>()

    private var sizeItems = 0
    private var sizePages = 0

    private fun trimToMaxSize() {
        while (itemMemoryCache.size > maxSize) {
            val oldestKey = itemMemoryCache.keys.firstOrNull()
            if (oldestKey != null) {
                removeItem(oldestKey)
            } else {
                break
            }
        }

    }

    private fun removeItem(id: Quantifiable<Id>) {
        val key = idToKeyMap[id]

        if (key != null) {
            val pageItemIds = pageMemoryCache[key]
            if (pageItemIds != null) {
                val mutableListCopy = pageItemIds.toMutableList()
                mutableListCopy.remove(id)
                if (mutableListCopy.isEmpty()) {
                    // Remove the page too
                    removePage(key)
                } else {
                    pageMemoryCache[key] = mutableListCopy
                }

            }
        }

        // TODO(): Only encode/decode if db != null
        val encodedItemId = Json.encodeToString(registry.id.serializer(), id.value)

        db?.itemQueries?.removeItem(encodedItemId)

        itemMemoryCache.remove(id)
        sizeItems--
    }


    private fun removePageNode(node: PageNode<K>) {
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
        pageNodeMap.remove(node.key)

        val pageItemIds = pageMemoryCache[node.key]

        pageItemIds?.forEach { id ->
            removeItem(id)
        }

        pageMemoryCache.remove(node.key)
        val encodedParams = Json.encodeToString(
            PagingSource.LoadParams.serializer(registry.key.serializer()),
            keyToParamsMap[node.key]!!
        )
        db?.pageQueries?.removePage(encodedParams)

        sizePages--
    }

    private fun removePage(key: K) {
        pageNodeMap[key]?.let { node ->
            removePageNode(node)
        }
    }

    private fun onSnapshot(snapshot: ItemSnapshotList<Id, Q, V>) {
        sideEffects.forEach { sideEffect -> sideEffect.invoke(snapshot) }
    }

    private fun prependItem(item: V, localParams: String) {
        // save items to memory cache

        itemMemoryCache[item.id] = item
        sizeItems++

        // save items to database
        saveItemToDb(item, localParams)
    }

    private fun appendItem(item: V, localParams: String) {
        // save items to memory cache

        itemMemoryCache[item.id] = item
        sizeItems++

        // save items to database
        saveItemToDb(item, localParams)
    }

    private fun prependPlaceholders(
        params: PagingSource.LoadParams<K>
    ) {

        val pageNode = PageNode(key = params.key, placeholder = true)
        if (headPage == null) {
            headPage = pageNode
            tailPage = pageNode
        } else {
            headPage?.prev = pageNode
            pageNode.next = headPage
            headPage = pageNode
        }

        pageMemoryCache[params.key] = List(pagingConfig.pageSize) { pagingConfig.placeholderId!! }
        pageNodeMap[params.key] = pageNode
        sizePages++
        trimToMaxSize()
    }

    private fun appendPlaceholders(
        params: PagingSource.LoadParams<K>
    ) {
        val pageNode = PageNode(key = params.key)

        if (tailPage == null) {
            headPage = pageNode
            tailPage = pageNode
        } else {
            tailPage?.next = pageNode
            pageNode.prev = tailPage
            tailPage = pageNode
        }

        pageMemoryCache[params.key] = List(pagingConfig.pageSize) { pagingConfig.placeholderId!! }
        pageNodeMap[params.key] = pageNode
        sizePages++
    }

    private fun prependPage(
        params: PagingSource.LoadParams<K>,
        localParams: String,
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
            node.copy(placeholder = false)
        }

        pageMemoryCache[params.key] = fetcherResult.value.items.map { it.id }
        pageNodeMap[params.key] = pageNode
        sizePages++

        // save normalized page to database
        saveNormalizedPageToDb(localParams, fetcherResult)
    }

    private fun appendPage(
        params: PagingSource.LoadParams<K>,
        localParams: String,
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
            node.copy(placeholder = false)
        }


        pageMemoryCache[params.key] = fetcherResult.value.items.map { it.id }
        pageNodeMap[params.key] = pageNode
        sizePages++

        // save normalized page to database
        saveNormalizedPageToDb(localParams, fetcherResult)
    }


    private fun saveItemToDb(item: V, localParams: String) {
        val id = Json.encodeToString(registry.id.serializer(), item.id.value)


        val localItem = Item(
            id,
            Json.encodeToString(registry.value.serializer(), item),
            localParams
        )

        db?.itemQueries?.setItem(localItem)
    }

    private fun saveNormalizedPageToDb(
        localParams: String,
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, Q, K, V, E>>
    ) {
        val localPage = Page(
            params = localParams,

            nextKey = fetcherResult.value.nextKey?.let {
                Json.encodeToString(
                    registry.key.serializer(),
                    it
                )
            },
            prevKey = fetcherResult.value.prevKey?.let {
                Json.encodeToString(
                    registry.key.serializer(),
                    it
                )
            },
            extras = fetcherResult.value.extras?.toString()
        )

        db?.pageQueries?.setPage(localPage)
    }


    private suspend fun loadFromNetwork(params: PagingSource.LoadParams<K>): PageLoadStatus<Id, Q, K, V, E> {
        println("HITTING IN LOAD FROM NETWORK")

        return when (val fetcherResult = pageFetcher.invoke(params).first()) {
            is FetcherResult.Data -> {
                println("HITTING IN LOAD FROM NETWORK DATA")

                val items = fetcherResult.value.items

                println("ITEMS = $items")


                val localParams = try {
                    Json.encodeToString(
                        PagingSource.LoadParams.serializer(registry.key.serializer()),
                        params
                    )
                } catch (error: Throwable) {
                    println("ERROR = $error")
                    throw error
                }
                println("LOCAL PARAMS = $localParams")


                when (params.direction) {
                    LoadDirection.Prepend -> {
                        items.asReversed().forEach { item ->
                            prependItem(item, localParams)
                        }

                        // save normalized page to memory cache
                        prependPage(params, localParams, fetcherResult)
                    }

                    LoadDirection.Append -> {
                        println("HITTING IN APPEND")
                        items.forEach { item ->
                            appendItem(item, localParams)
                        }

                        // save normalized page to memory cache
                        appendPage(params, localParams, fetcherResult)
                    }
                }

                trimToMaxSize()

                PageLoadStatus.Success(
                    snapshot = snapshot(),
                    isTerminal = true,
                    source = PageLoadStatus.Success.Source.Network,
                    nextKey = fetcherResult.value.nextKey,
                    prevKey = fetcherResult.value.prevKey ?: pageNodeMap[params.key]?.prev?.key
                )


            }

            is FetcherResult.Error.Exception -> {
                println("HITTING IN LOAD FROM NETWORK ERROR")
                if (fetcherResult.error is PagingError) {
                    val pagingError = fetcherResult.error as PagingError
                    val error = Json.decodeFromString(
                        registry.error.serializer(),
                        pagingError.encodedError
                    )
                    val extras = pagingError.extras
                    PageLoadStatus.Error(error, extras, true)
                } else {
                    val error = errorFactory.create(fetcherResult.error)

                    PageLoadStatus.Error(
                        error,
                        fetcherResult.error.extras(),
                        true

                    )
                }

            }

            is FetcherResult.Error.Custom<*> -> {
                println("HITTING IN LOAD FROM NETWORK ERROR")
                val error = fetcherResult.error as E
                PageLoadStatus.Error(error, null, true)
            }

            is FetcherResult.Error.Message -> {
                println("HITTING IN LOAD FROM NETWORK ERROR")
                val error = errorFactory.create(fetcherResult.message)
                PageLoadStatus.Error(error, null, true)
            }
        }
    }

    override fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadStatus<Id, Q, K, V, E>> =
        flow {
            println("PARAMS = $params")
            keyToParamsMap[params.key] = params

            if (pagingConfig.placeholderId != null && params.strategy != LoadStrategy.LocalOnly) {
                // Add placeholders
                when (params.direction) {
                    LoadDirection.Prepend -> {
                        prependPlaceholders(params)
                    }

                    LoadDirection.Append -> {
                        appendPlaceholders(params)
                    }
                }
            }

            fun inFlight(key: K): Boolean {
                return pageNodeMap[key]?.placeholder == true
            }

            fun isCached(key: K): Boolean {
                return pageMemoryCache[key]?.isNotEmpty() == true
            }

            fun inDatabase(params: PagingSource.LoadParams<K>): Boolean {
                val localParams = Json.encodeToString(
                    PagingSource.LoadParams.serializer(registry.key.serializer()),
                    params
                )
                return db?.pageQueries?.getPage(localParams)?.executeAsOneOrNull() != null
            }

            // emit loading
            emit(PageLoadStatus.Processing())

            // fetch page

            println("GOING TO FETCH PAGE")
            when (params.strategy) {
                is LoadStrategy.CacheFirst -> {

                    if (inFlight(params.key)) {
                        emit(PageLoadStatus.SkippingLoad.inFlight())
                    } else {

                        if (isCached(params.key)) {
                            emit(PageLoadStatus.Loading.memoryCache())

                            emit(
                                PageLoadStatus.Success(
                                    snapshot = snapshot(),
                                    isTerminal = false,
                                    source = PageLoadStatus.Success.Source.MemoryCache,
                                    nextKey = pageNodeMap[params.key]?.next?.key,
                                    prevKey = pageNodeMap[params.key]?.prev?.key
                                )
                            )
                        } else if (inDatabase(params)) {
                            emit(PageLoadStatus.Loading.database())

                            emit(
                                PageLoadStatus.Success(
                                    snapshot = snapshot(),
                                    isTerminal = false,
                                    source = PageLoadStatus.Success.Source.Database,
                                    nextKey = pageNodeMap[params.key]?.next?.key,
                                    prevKey = pageNodeMap[params.key]?.prev?.key
                                )
                            )

                        }

                        emit(PageLoadStatus.Loading.remote())

                        val status = loadFromNetwork(params)
                        emit(status)
                    }
                }

                LoadStrategy.SkipCache -> {
                    println("HITTING")

                    if (inFlight(params.key)) {
                        emit(PageLoadStatus.SkippingLoad.inFlight())
                    } else {
                        emit(PageLoadStatus.Loading.remote())

                        val status = loadFromNetwork(params)
                        println("STATUS = $status")
                        emit(status)
                    }

                }

                LoadStrategy.LocalOnly -> {
                    if (inFlight(params.key)) {
                        emit(PageLoadStatus.SkippingLoad.inFlight())
                    } else {
                        if (isCached(params.key)) {
                            emit(
                                PageLoadStatus.Success(
                                    snapshot = snapshot(),
                                    isTerminal = true,
                                    source = PageLoadStatus.Success.Source.MemoryCache,
                                    nextKey = pageNodeMap[params.key]?.next?.key,
                                    prevKey = pageNodeMap[params.key]?.prev?.key
                                )
                            )
                        } else if (inDatabase(params)) {
                            emit(PageLoadStatus.Loading.database())

                            emit(
                                PageLoadStatus.Success(
                                    snapshot = snapshot(),
                                    isTerminal = false,
                                    source = PageLoadStatus.Success.Source.Database,
                                    nextKey = pageNodeMap[params.key]?.next?.key,
                                    prevKey = pageNodeMap[params.key]?.prev?.key
                                )
                            )

                        } else {
                            emit(
                                PageLoadStatus.Empty(
                                    true,
                                    PageLoadStatus.Empty.Reason.LocalOnlyRequest
                                )
                            )
                        }
                    }
                }

                LoadStrategy.Refresh -> {
                    // TODO(): Update - do we need this? can just configure with others (e.g., invalidate or skip queue with prepend)
                    // TODO(): Support refresh
                }
            }

        }

    override fun selfUpdatingItem(id: Q): SelfUpdatingItem<Id, Q, V, E> {
        val presenter = @Composable { events: Flow<SelfUpdatingItem.Event<Id, Q, V, E>> ->
            selfUpdatingItem(id, events)
        }

        return SelfUpdatingItem(presenter)
    }

    @Composable
    private fun selfUpdatingItem(
        id: Q,
        events: Flow<SelfUpdatingItem.Event<Id, Q, V, E>>
    ): ItemState<Id, Q, V, E> {
        val encodedId = remember(id) { Json.encodeToString(registry.id.serializer(), id.value) }

        val v by remember {
            derivedStateOf { itemMemoryCache[id] }
        }

        var itemVersion by remember(id) {
            mutableStateOf(0L)
        }

        var singleLoadState: SingleLoadState<E> by remember(id) {
            val item = v
            mutableStateOf(

                // This is the initial state, because [id] is the key

                if (item != null) {
                    val nextItemVersion = itemVersion + 1
                    itemVersion = nextItemVersion
                    SingleLoadState.Loaded
                } else {
                    SingleLoadState.Initial
                }
            )
        }


        val state by remember(v, singleLoadState, itemVersion) {
            derivedStateOf {
                ItemState(v, singleLoadState, itemVersion)
            }
        }

        LaunchedEffect(id) {
            fetchingStateHolder.updateMaxItemAccessedSoFar(id)
        }

        LaunchedEffect(id, events) {
            events.collect { event ->
                when (event) {
                    is SelfUpdatingItem.Event.Clear -> {
                        // Remove from memory cache
                        // Remove from database
                        removeItem(id)
                        itemVersion = 0L
                        singleLoadState = SingleLoadState.Cleared

                        // TODO(): Support network
                    }

                    is SelfUpdatingItem.Event.Refresh -> {
                        // Load from network

                        singleLoadState = SingleLoadState.Refreshing

                        if (itemFetcher == null) {
                            throw IllegalStateException("Item fetcher is null")
                        }

                        when (val fetcherResult = itemFetcher.invoke(id.value).first()) {
                            is FetcherResult.Data -> {
                                val item = fetcherResult.value

                                // Update memory cache
                                itemMemoryCache[item.id] = item

                                // Update database
                                val itemId =
                                    Json.encodeToString(registry.id.serializer(), item.id.value)
                                val updatedData =
                                    Json.encodeToString(registry.value.serializer(), item)
                                db?.itemQueries?.updateItem(updatedData, itemId)

                                // Update state
                                // TODO(): Should I recheck the state?
                                singleLoadState = SingleLoadState.Loaded
                                itemVersion.inc()
                            }

                            is FetcherResult.Error.Custom<*> -> {
                                val error = fetcherResult.error as E
                                singleLoadState = SingleLoadState.Error.Refreshing(error)

                            }

                            is FetcherResult.Error.Exception -> {
                                if (fetcherResult.error is PagingError) {
                                    val pagingError = fetcherResult.error as PagingError
                                    val error = Json.decodeFromString(
                                        registry.error.serializer(),
                                        pagingError.encodedError
                                    )
                                    val extras = pagingError.extras


                                    singleLoadState = SingleLoadState.Error.Refreshing(
                                        error,
                                        extras
                                    )

                                } else {
                                    val error = errorFactory.create(fetcherResult.error)

                                    singleLoadState = SingleLoadState.Error.Refreshing(
                                        error,
                                        fetcherResult.error.extras()
                                    )

                                }
                            }

                            is FetcherResult.Error.Message -> {
                                val error = errorFactory.create(fetcherResult.message)

                                singleLoadState = SingleLoadState.Error.Refreshing(
                                    error,
                                    null
                                )
                            }
                        }


                    }

                    is SelfUpdatingItem.Event.Update -> {
                        // Save to memory cache
                        itemMemoryCache[id] = event.value

                        // Save to database
                        val itemId =
                            Json.encodeToString(registry.id.serializer(), event.value.id.value)
                        val updatedData =
                            Json.encodeToString(registry.value.serializer(), event.value)
                        db?.itemQueries?.updateItem(updatedData, itemId)

                        // TODO(): Support network

                        singleLoadState = SingleLoadState.Loaded
                        itemVersion.inc()
                    }
                }
            }
        }

        LaunchedEffect(id) {
            if (db == null) {
                return@LaunchedEffect
            }

            db.itemQueries.getItem(encodedId).asFlow().map { query ->
                val item = query.executeAsOneOrNull()

                if (item != null) {
                    Json.decodeFromString(registry.value.serializer(), item.data_)
                } else {
                    null
                }
            }.collect { item ->
                if (item == null) {

                    if (v != null) {
                        // Remove the item
                        removeItem(id)
                        itemVersion = 0L
                        singleLoadState = SingleLoadState.Cleared
                    }

                    // TODO(): When else does this happen


                } else if (item != v) {
                    // Save to memory cache, just in case
                    itemMemoryCache[id] = item

                    // TODO(): Support network

                    singleLoadState = SingleLoadState.Loaded
                    itemVersion.inc()
                }
            }


        }
        return state
    }


    override fun selfUpdatingPage(key: K): SelfUpdatingPage<Id, Q, K, V, E> {
        TODO()
    }

    private fun cachedPagingItems(key: K): List<V?> {
        return pageMemoryCache[key]!!.let { ids ->
            ids.map { id ->
                if (id == pagingConfig.placeholderId) {
                    null
                } else {
                    itemMemoryCache[id]!!
                }
            }
        }
    }

    private fun getItemsInOrder(): List<V?> {
        val items = mutableListOf<V?>()

        var current = headPage

        while (current != null) {
            items.addAll(cachedPagingItems(current.key))
            current = current.next
        }

        return items
    }

    private fun snapshot(): ItemSnapshotList<Id, Q, V> {
        return ItemSnapshotList(getItemsInOrder()).also {
            onSnapshot(it)
        }
    }


    override fun invalidate() {
        // Clear memory caches
        idToKeyMap.clear()
        keyToParamsMap.clear()
        pageNodeMap.clear()
        pageMemoryCache.clear()
        itemMemoryCache.clear()

        // Reset head and tail pages
        headPage = null
        tailPage = null

        // Reset size counters
        sizeItems = 0
        sizePages = 0

        // Clear the database (if applicable)
        db?.let {
            // it.itemQueries.removeAllItems()
            // it.pageQueries.removeAllPages()
        }
    }

    override fun clear(key: K) {
        removePage(key)
    }


}