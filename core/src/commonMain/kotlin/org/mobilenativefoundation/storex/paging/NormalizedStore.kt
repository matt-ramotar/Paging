package org.mobilenativefoundation.storex.paging

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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.db.DriverFactory
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.internal.impl.KClassRegistry
import org.mobilenativefoundation.storex.paging.internal.impl.PagingError


@Suppress("UNCHECKED_CAST")
@OptIn(InternalSerializationApi::class)
class RealNormalizedStore<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
    private val pagingSource: PagingSource<Id, K, V, E>,
    private val registry: KClassRegistry<Id, K, V, E>,
    private val errorFactory: ErrorFactory<E>,
    private val itemFetcher: Fetcher<Id, V>,
    driverFactory: DriverFactory,
    private val maxSize: Int = 500,
    private val fetchingStateHolder: FetchingStateHolder<Id>,
    private val sideEffects: List<SideEffect<Id, V>>,
    private val pagingConfig: PagingConfig<Id>
) : NormalizedStore<Id, K, V, E> {

    private val db = PagingDb(driverFactory.createDriver())

    private data class PageNode<K : Any>(
        val key: K,
        val placeholder: Boolean = false,
        var prev: PageNode<K>? = null,
        var next: PageNode<K>? = null
    )

    private data class ItemNode<Id : Comparable<Id>>(
        val id: Id,
        var prev: ItemNode<Id>? = null,
        var next: ItemNode<Id>? = null
    )

    private val idToKeyMap = mutableMapOf<Id, K>()
    private val keyToParamsMap = mutableMapOf<K, PagingSource.LoadParams<K>>()

    private var headPage: PageNode<K>? = null
    private var tailPage: PageNode<K>? = null

    private var headItem: ItemNode<Id>? = null
    private var tailItem: ItemNode<Id>? = null

    private val itemNodeMap = mutableMapOf<Id, ItemNode<Id>>()
    private val pageNodeMap = mutableMapOf<K, PageNode<K>>()

    private var pageMemoryCache = mutableMapOf<K, List<Id>>()
    private var itemMemoryCache = mutableMapOf<Id, V>()

    private var sizeItems = 0
    private var sizePages = 0


    private val pageFetcher =
        Fetcher.ofResult<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>> {
            when (val loadResult = pagingSource.load(it)) {
                is PagingSource.LoadResult.Data -> FetcherResult.Data(loadResult)
                is PagingSource.LoadResult.Error -> {
                    FetcherResult.Error.Exception(
                        PagingError(
                            encodedError = Json.encodeToString(
                                registry.error.serializer(),
                                loadResult.error
                            ),
                            extras = loadResult.extras
                        )
                    )
                }
            }
        }


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

    private fun removeItem(id: Id) {
        itemNodeMap[id]?.let { node ->
            removeItemNode(node)

        }
    }

    private fun removeItemNode(node: ItemNode<Id>) {

        if (node.id !in itemNodeMap) {
            return
        }

        if (node.prev == null) {
            headItem = node.next
        } else {
            node.prev?.next = node.next
        }
        if (node.next == null) {
            tailItem = node.prev
        } else {
            node.next?.prev = node.prev
        }
        itemNodeMap.remove(node.id)

        val key = idToKeyMap[node.id]

        if (key != null) {
            val pageItemIds = pageMemoryCache[key]
            if (pageItemIds != null) {
                val mutableListCopy = pageItemIds.toMutableList()
                mutableListCopy.remove(node.id)
                if (mutableListCopy.isEmpty()) {
                    // Remove the page too
                    removePage(key)
                } else {
                    pageMemoryCache[key] = mutableListCopy
                }

            }
        }

        val encodedItemId = Json.encodeToString(registry.id.serializer(), node.id)
        db.itemQueries.removeItem(encodedItemId)

        itemMemoryCache.remove(node.id)
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
        db.pageQueries.removePage(encodedParams)

        sizePages--
    }

    private fun removePage(key: K) {
        pageNodeMap[key]?.let { node ->
            removePageNode(node)
        }
    }

    private fun onSnapshot(snapshot: ItemSnapshotList<Id, V>) {
        sideEffects.forEach { sideEffect -> sideEffect.invoke(snapshot) }
    }

    private fun prependItem(item: V, localParams: String) {
        // save items to memory cache

        val itemNode = ItemNode(item.id)

        if (headItem == null) {
            headItem = itemNode
            tailItem = itemNode
        } else {
            headItem?.prev = itemNode
            itemNode.next = headItem
            headItem = itemNode
        }

        itemMemoryCache[item.id] = item
        itemNodeMap[item.id] = itemNode
        sizeItems++

        // save items to database
        saveItemToDb(item, localParams)
    }

    private fun appendItem(item: V, localParams: String) {
        // save items to memory cache

        val itemNode = ItemNode(item.id)
        if (tailItem == null) {
            headItem = itemNode
            tailItem = itemNode
        } else {
            tailItem?.next = itemNode
            itemNode.prev = tailItem
            tailItem = itemNode
        }
        itemMemoryCache[item.id] = item
        itemNodeMap[item.id] = itemNode
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
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, K, V, E>>
    ) {
        val pageNode = PageNode(key = params.key)

        if (headPage == null) {
            headPage = pageNode
            tailPage = pageNode
        } else {
            headPage?.prev = pageNode
            pageNode.next = headPage
            headPage = pageNode
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
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, K, V, E>>
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
        pageMemoryCache[params.key] = fetcherResult.value.items.map { it.id }
        pageNodeMap[params.key] = pageNode
        sizePages++

        // save normalized page to database
        saveNormalizedPageToDb(localParams, fetcherResult)
    }


    private fun saveItemToDb(item: V, localParams: String) {
        val id = Json.encodeToString(registry.id.serializer(), item.id)


        val localItem = Item(
            id,
            Json.encodeToString(registry.value.serializer(), item),
            localParams
        )

        db.itemQueries.setItem(localItem)
    }

    private fun saveNormalizedPageToDb(
        localParams: String,
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, K, V, E>>
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

        db.pageQueries.setPage(localPage)
    }


    private suspend fun loadFromNetwork(params: PagingSource.LoadParams<K>): LoadPageStatus<Id, K, V, E> {
        return when (val fetcherResult = pageFetcher(params).first()) {
            is FetcherResult.Data -> {

                val items = fetcherResult.value.items

                val localParams = Json.encodeToString(
                    PagingSource.LoadParams.serializer(registry.key.serializer()),
                    params
                )


                when (params.direction) {
                    PagingSource.LoadParams.Direction.Prepend -> {
                        items.asReversed().forEach { item ->
                            prependItem(item, localParams)
                        }

                        // save normalized page to memory cache
                        prependPage(params, localParams, fetcherResult)
                    }

                    PagingSource.LoadParams.Direction.Append -> {
                        items.forEach { item ->
                            appendItem(item, localParams)
                        }

                        // save normalized page to memory cache
                        appendPage(params, localParams, fetcherResult)
                    }
                }

                trimToMaxSize()

                LoadPageStatus.Success(
                    items = items,
                    snapshot = snapshot(),
                    terminal = true,
                    source = LoadPageStatus.Success.Source.Network
                )


            }

            is FetcherResult.Error.Exception -> {
                if (fetcherResult.error is PagingError) {
                    val pagingError = fetcherResult.error as PagingError
                    val error = Json.decodeFromString(
                        registry.error.serializer(),
                        pagingError.encodedError
                    )
                    val extras = pagingError.extras
                    LoadPageStatus.Error(error, extras, true)
                } else {
                    val error = errorFactory.create(fetcherResult.error)

                    LoadPageStatus.Error(
                        error,
                        fetcherResult.error.extras(),
                        true

                    )
                }

            }

            is FetcherResult.Error.Custom<*> -> {
                val error = fetcherResult.error as E
                LoadPageStatus.Error(error, null, true)
            }

            is FetcherResult.Error.Message -> {
                val error = errorFactory.create(fetcherResult.message)
                LoadPageStatus.Error(error, null, true)
            }
        }
    }

    override fun loadPage(params: PagingSource.LoadParams<K>): Flow<LoadPageStatus<Id, K, V, E>> =
        flow {

            keyToParamsMap[params.key] = params

            if (pagingConfig.placeholderId != null && params.strategy != PagingSource.LoadParams.Strategy.LocalOnly) {
                // Add placeholders
                when (params.direction) {
                    PagingSource.LoadParams.Direction.Prepend -> {
                        prependPlaceholders(params)
                    }

                    PagingSource.LoadParams.Direction.Append -> TODO()
                }
            }

            fun inFlight(key: K): Boolean {
                TODO()
            }

            fun isCached(key: K): Boolean {
                TODO()
            }

            fun inDatabase(key: K): Boolean {
                TODO()
            }

            // emit loading
            emit(LoadPageStatus.Processing<Id, K, V, E>())

            // fetch page

            when (params.strategy) {
                is PagingSource.LoadParams.Strategy.CacheFirst -> {

                    if (inFlight(params.key)) {
                        emit(LoadPageStatus.SkippingLoad.inFlight())
                    } else {

                        if (isCached(params.key)) {
                            emit(LoadPageStatus.Loading.memoryCache())

                            val items = cachedPagingItems(params.key)
                            emit(
                                LoadPageStatus.Success(
                                    items = items,
                                    snapshot = snapshot(),
                                    terminal = false,
                                    source = LoadPageStatus.Success.Source.MemoryCache
                                )
                            )
                        } else if (inDatabase(params.key)) {
                            emit(LoadPageStatus.Loading.database())

                            val items = persistedPagingItems(params.key)
                            emit(
                                LoadPageStatus.Success(
                                    items = items,
                                    snapshot = snapshot(),
                                    terminal = false,
                                    source = LoadPageStatus.Success.Source.Database
                                )
                            )

                        }

                        emit(LoadPageStatus.Loading.remote())

                        val status = loadFromNetwork(params)
                        emit(status)
                    }
                }

                PagingSource.LoadParams.Strategy.SkipCache -> {

                    if (inFlight(params.key)) {
                        emit(LoadPageStatus.SkippingLoad.inFlight())
                    } else {
                        emit(LoadPageStatus.Loading.remote())

                        val status = loadFromNetwork(params)
                        emit(status)
                    }

                }

                PagingSource.LoadParams.Strategy.LocalOnly -> {
                    if (inFlight(params.key)) {
                        emit(LoadPageStatus.SkippingLoad.inFlight())
                    } else {
                        if (isCached(params.key)) {
                            val items = cachedPagingItems(params.key)
                            emit(
                                LoadPageStatus.Success(
                                    items = items,
                                    snapshot = snapshot(),
                                    terminal = true,
                                    source = LoadPageStatus.Success.Source.MemoryCache
                                )
                            )
                        } else if (inDatabase(params.key)) {
                            emit(LoadPageStatus.Loading.database())

                            val items = persistedPagingItems(params.key)
                            emit(
                                LoadPageStatus.Success(
                                    items = items,
                                    snapshot = snapshot(),
                                    terminal = false,
                                    source = LoadPageStatus.Success.Source.Database
                                )
                            )

                        } else {
                            emit(
                                LoadPageStatus.Empty(
                                    true,
                                    LoadPageStatus.Empty.Reason.LocalOnlyRequest
                                )
                            )
                        }
                    }
                }
            }

        }

    override fun selfUpdatingItem(id: Id): SelfUpdatingItem<Id, V, E> {
        val presenter = @Composable { events: Flow<SelfUpdatingItemEvent<Id, V, E>> ->
            selfUpdatingItem(id, events)
        }

        return SelfUpdatingItem(presenter)
    }

    @Composable
    private fun selfUpdatingItem(
        id: Id,
        events: Flow<SelfUpdatingItemEvent<Id, V, E>>
    ): ItemState<Id, V, E> {
        val encodedId = remember(id) { Json.encodeToString(registry.id.serializer(), id) }

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
                ItemState<Id, V, E>(v, singleLoadState, itemVersion)
            }
        }

        LaunchedEffect(id) {
            fetchingStateHolder.updateMaxItemAccessedSoFar(id)
        }

        LaunchedEffect(id, events) {
            events.collect { event ->
                when (event) {
                    is SelfUpdatingItemEvent.Clear -> {
                        // Remove from memory cache
                        // Remove from database
                        removeItem(id)
                        itemVersion = 0L
                        singleLoadState = SingleLoadState.Cleared

                        // TODO(): Support network
                    }

                    is SelfUpdatingItemEvent.Refresh -> {
                        // Load from network

                        val prevState = state
                        singleLoadState = SingleLoadState.Refreshing

                        when (val fetcherResult = itemFetcher(id).first()) {
                            is FetcherResult.Data -> {
                                val item = fetcherResult.value

                                // Update memory cache
                                itemMemoryCache[item.id] = item

                                // Update database
                                val itemId = Json.encodeToString(registry.id.serializer(), item.id)
                                val updatedData =
                                    Json.encodeToString(registry.value.serializer(), item)
                                db.itemQueries.updateItem(updatedData, itemId)

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

                    is SelfUpdatingItemEvent.Update -> {
                        // Save to memory cache
                        itemMemoryCache[id] = event.value

                        // Save to database
                        val itemId = Json.encodeToString(registry.id.serializer(), event.value.id)
                        val updatedData =
                            Json.encodeToString(registry.value.serializer(), event.value)
                        db.itemQueries.updateItem(updatedData, itemId)

                        // TODO(): Support network

                        singleLoadState = SingleLoadState.Loaded
                        itemVersion.inc()
                    }
                }
            }
        }

        LaunchedEffect(id) {
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


    override fun selfUpdatingPage(key: K): SelfUpdatingPage<Id, K, V, E> {
        TODO()
    }

    private fun cachedPagingItem(id: Id): V {
        return itemMemoryCache[id]!!
    }

    private fun cachedPagingItems(key: K): List<V> {
        return pageMemoryCache[key]!!.let { ids ->
            ids.map { id ->
                itemMemoryCache[id]!!
            }
        }
    }

    private fun getItemsInOrder(): List<V> {
        val items = mutableListOf<V>()

        var current = headPage

        while (current != null) {
            items.addAll(cachedPagingItems(current.key))
            current = current.next
        }

        return items
    }

    private fun snapshot(): ItemSnapshotList<Id, V> {
        return ItemSnapshotList(getItemsInOrder()).also {
            onSnapshot(it)
        }
    }

    private suspend fun persistedPagingItems(key: K): List<V> {
        TODO()
    }

    private suspend fun persistedPagingItem(id: Id): V {
        TODO()
    }

    override fun invalidate() {
        TODO()
    }

    override fun clear(key: K) {
        removePage(key)
    }


}

sealed interface LoadPageStatus<Id : Comparable<Id>, out K : Any, out V : Identifiable<Id>, out E : Any> {

    val terminal: Boolean

    data class Processing<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        override val terminal: Boolean = false
    ) : LoadPageStatus<Id, K, V, E>

    data class Loading<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        override val terminal: Boolean = false,
        val source: Source
    ) : LoadPageStatus<Id, K, V, E> {
        enum class Source {
            MemoryCache,
            Database,
            Remote
        }

        companion object {
            fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> memoryCache() =
                Loading<Id, K, V, E>(
                    source = Source.MemoryCache
                )

            fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> database() =
                Loading<Id, K, V, E>(
                    source = Source.Database
                )

            fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> remote() =
                Loading<Id, K, V, E>(
                    source = Source.Remote
                )
        }
    }


    data class SkippingLoad<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        val reason: Reason,
        override val terminal: Boolean = true
    ) : LoadPageStatus<Id, K, V, E> {
        enum class Reason {
            AlreadyInFlight
        }

        companion object {
            fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> inFlight() =
                SkippingLoad<Id, K, V, E>(
                    Reason.AlreadyInFlight
                )
        }
    }

    data class Success<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        override val terminal: Boolean = true,
        val items: List<V>,
        val snapshot: ItemSnapshotList<Id, V>,
        val prevKey: K? = null,
        val nextKey: K? = null,
        val source: Source
    ) : LoadPageStatus<Id, K, V, E> {
        enum class Source {
            MemoryCache,
            Database,
            Network
        }
    }

    data class Error<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        val error: E,
        val extras: JsonObject? = null,
        override val terminal: Boolean
    ) : LoadPageStatus<Id, K, V, E>

    data class Empty<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        override val terminal: Boolean = true,
        val reason: Reason
    ) : LoadPageStatus<Id, K, V, E> {
        enum class Reason {
            LocalOnlyRequest
        }
    }

}


fun Throwable.extras(): JsonObject = JsonObject(
    mapOf(
        "message" to JsonPrimitive(message.orEmpty()),
        "stackTrace" to JsonPrimitive(stackTraceToString()),
        "cause" to JsonPrimitive(cause?.message.orEmpty())
    )

)


sealed interface SelfUpdatingItemEvent<Id : Comparable<Id>, V : Identifiable<Id>, E : Any> {

    data class Refresh<Id : Comparable<Id>, V : Identifiable<Id>, E : Any>(
        val message: String? = null,
    ) : SelfUpdatingItemEvent<Id, V, E>

    data class Update<Id : Comparable<Id>, V : Identifiable<Id>, E : Any>(
        val value: V,
    ) : SelfUpdatingItemEvent<Id, V, E>

    data class Clear<Id : Comparable<Id>, V : Identifiable<Id>, E : Any>(
        val message: String? = null
    ) : SelfUpdatingItemEvent<Id, V, E>
}