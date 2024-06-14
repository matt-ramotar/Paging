package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.api.NormalizedStore

typealias PageLoadStatusFlow<Id, Q, K, V, E> = FlowCollector<PageLoadStatus<Id, Q, K, V, E>>

typealias WithDb = ((suspend (PagingDb) -> Unit) -> Unit)

@Suppress("UNCHECKED_CAST")
@OptIn(InternalSerializationApi::class)
class ConcurrentNormalizedStore<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    private val pageFetcher: Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, Q, K, V, E>>,
    private val registry: KClassRegistry<Id, Q, K, V, E>,
    private val errorFactory: ErrorFactory<E>,
    private val db: PagingDb? = null,
    private val fetchingStateHolder: FetchingStateHolder<Id, Q, K>,
    private val sideEffects: List<SideEffect<Id, Q, V>>,
    private val pagingConfig: PagingConfig<Id, Q, K>,
    private val selfUpdatingItemPresenter: SelfUpdatingItemPresenter<Id, Q, K, V, E>,
    private val pagingLinkedHashMap: PagingLinkedHashMap<Id, Q, K, V, E>,
) : NormalizedStore<Id, Q, K, V, E> {

    override fun selfUpdatingItem(id: Q): SelfUpdatingItem<Id, Q, V, E> {
        return SelfUpdatingItem { events -> selfUpdatingItemPresenter.present(id, events) }
    }

    override fun selfUpdatingPage(key: K): SelfUpdatingPage<Id, Q, K, V, E> {
        TODO("Not yet implemented")
    }

    override fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadStatus<Id, Q, K, V, E>> = flow {

        // If needed, add placeholders, before loading
        if (shouldAddPlaceholders(params)) {
            when (params.direction) {
                LoadDirection.Prepend -> pagingLinkedHashMap.prependPlaceholders(params)
                LoadDirection.Append -> pagingLinkedHashMap.appendPlaceholders(params)
            }
        }

        // Emit processing status, before loading
        emit(PageLoadStatus.Processing())

        handleLoading(params)
    }

    override fun getItem(id: Q): V? {
        return pagingLinkedHashMap.getItem(id) ?: getItemFromDb(id)
    }

    private fun getItemFromDb(id: Q): V? {

        if (db != null) {
            val encodedId = Json.encodeToString(registry.q.serializer(), id)
            val encodedItem = db.itemQueries.getItem(encodedId).executeAsOneOrNull()
            return encodedItem?.let { Json.decodeFromString(registry.value.serializer(), it.data_) }
        }

        return null
    }


    override fun clear(key: K) {
        pagingLinkedHashMap.removePage(key)
    }

    override fun invalidate() {
        pagingLinkedHashMap.invalidate()
    }

    private fun shouldAddPlaceholders(params: PagingSource.LoadParams<K>): Boolean {
        val placeholdersAreEnabled = pagingConfig.placeholderId != null
        val mightFetchFromRemote = params.strategy != LoadStrategy.LocalOnly
        return placeholdersAreEnabled && mightFetchFromRemote
    }


    private suspend fun PageLoadStatusFlow<Id, Q, K, V, E>.handleLoading(
        params: PagingSource.LoadParams<K>
    ) {
        when (params.strategy) {
            is LoadStrategy.CacheFirst -> handleCacheFirstLoad(params)
            LoadStrategy.LocalOnly -> handleLocalOnlyLoad(params)
            LoadStrategy.SkipCache -> handleSkipCacheLoad(params)
        }
    }

    private suspend fun PageLoadStatusFlow<Id, Q, K, V, E>.handleCacheFirstLoad(
        params: PagingSource.LoadParams<K>
    ) {
        if (pagingLinkedHashMap.isInFlight(params.key)) {
            emit(PageLoadStatus.SkippingLoad.inFlight())
        } else {
            if (pagingLinkedHashMap.isCached(params.key)) {
                emit(PageLoadStatus.Loading.memoryCache())

                emit(
                    PageLoadStatus.Success(
                        snapshot = snapshot(),
                        isTerminal = true,
                        source = PageLoadStatus.Success.Source.MemoryCache,
                        nextKey = pagingLinkedHashMap.getPageNode(params.key)?.next?.key,
                        prevKey = pagingLinkedHashMap.getPageNode(params.key)?.prev?.key
                    )
                )
            } else if (pagingLinkedHashMap.isInDatabase(params)) {
                emit(PageLoadStatus.Loading.database())

                emit(
                    PageLoadStatus.Success(
                        snapshot = snapshot(),
                        isTerminal = true,
                        source = PageLoadStatus.Success.Source.Database,
                        nextKey = pagingLinkedHashMap.getPageNode(params.key)?.next?.key,
                        prevKey = pagingLinkedHashMap.getPageNode(params.key)?.prev?.key
                    )
                )
            }

            // Either way, we want to fetch from the remote data source
            handleFetchingFromRemote(params)
        }
    }

    private suspend fun PageLoadStatusFlow<Id, Q, K, V, E>.handleFetchingFromRemote(
        params: PagingSource.LoadParams<K>
    ) {
        emit(PageLoadStatus.Loading.remote())

        pagingLinkedHashMap.getPageNode(params.key)?.isInFlight = true

        return when (val fetcherResult = pageFetcher.invoke(params).first()) {
            is FetcherResult.Data -> handleFetcherResultData(params, fetcherResult)
            is FetcherResult.Error.Custom<*> -> handleCustomError(fetcherResult)
            is FetcherResult.Error.Exception -> handleException(fetcherResult)
            is FetcherResult.Error.Message -> handleErrorMessage(fetcherResult)
        }

    }

    private suspend fun PageLoadStatusFlow<Id, Q, K, V, E>.handleCustomError(fetcherResult: FetcherResult.Error.Custom<*>) {
        emit(PageLoadStatus.Error(fetcherResult.error as E, extras = null, isTerminal = true))
    }

    private suspend fun PageLoadStatusFlow<Id, Q, K, V, E>.handleException(fetcherResult: FetcherResult.Error.Exception) {
        val error = errorFactory.create(fetcherResult.error)
        emit(PageLoadStatus.Error(error, fetcherResult.error.extras(), isTerminal = true))
    }

    private suspend fun PageLoadStatusFlow<Id, Q, K, V, E>.handleErrorMessage(fetcherResult: FetcherResult.Error.Message) {
        val error = errorFactory.create(fetcherResult.message)
        emit(PageLoadStatus.Error(error, null, isTerminal = true))
    }

    private suspend fun PageLoadStatusFlow<Id, Q, K, V, E>.handleFetcherResultData(
        params: PagingSource.LoadParams<K>,
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, Q, K, V, E>>
    ) {
        if (fetcherResult.value.items.isEmpty()) {
            emit(PageLoadStatus.Empty(isTerminal = true, PageLoadStatus.Empty.Reason.NetworkResponse))
        } else {
            val encodedParams = Json.encodeToString(
                serializer = PagingSource.LoadParams.serializer(registry.key.serializer()),
                value = params
            )

            var maxItemLoaded = fetcherResult.value.items[0].id
            var minItemLoaded = fetcherResult.value.items[0].id

            when (params.direction) {
                LoadDirection.Prepend -> {
                    fetcherResult.value.items.asReversed().forEach { item ->
                        pagingLinkedHashMap.saveItem(item, encodedParams)

                        if (item.id.value < minItemLoaded.value) {
                            minItemLoaded = item.id
                        }

                        if (item.id.value > maxItemLoaded.value) {
                            maxItemLoaded = item.id
                        }
                    }

                    pagingLinkedHashMap.prependPage(params, encodedParams, fetcherResult)
                }

                LoadDirection.Append -> {
                    fetcherResult.value.items.forEach { item ->
                        pagingLinkedHashMap.saveItem(item, encodedParams)

                        if (item.id.value < minItemLoaded.value) {
                            minItemLoaded = item.id
                        }

                        if (item.id.value > maxItemLoaded.value) {
                            maxItemLoaded = item.id
                        }
                    }

                    pagingLinkedHashMap.appendPage(params, encodedParams, fetcherResult)
                }
            }

            fetchingStateHolder.updateMaxItemLoadedSoFar(maxItemLoaded)
            fetchingStateHolder.updateMinItemLoadedSoFar(minItemLoaded)

            emit(
                PageLoadStatus.Success(
                    snapshot = snapshot(),
                    isTerminal = true,
                    source = PageLoadStatus.Success.Source.Network,
                    nextKey = fetcherResult.value.nextKey,
                    prevKey = fetcherResult.value.prevKey ?: pagingLinkedHashMap.getPageNode(params.key)?.prev?.key
                )
            )
        }
    }

    private fun snapshot(): ItemSnapshotList<Id, Q, V> {
        return ItemSnapshotList(pagingLinkedHashMap.getItemsInOrder()).also {
            onSnapshot(it)
        }
    }

    private suspend fun PageLoadStatusFlow<Id, Q, K, V, E>.handleLocalOnlyLoad(
        params: PagingSource.LoadParams<K>
    ) {
        if (pagingLinkedHashMap.isInFlight(params.key)) {
            emit(PageLoadStatus.SkippingLoad.inFlight())
        } else {
            if (pagingLinkedHashMap.isCached(params.key)) {
                emit(
                    PageLoadStatus.Success(
                        snapshot = snapshot(),
                        isTerminal = true,
                        source = PageLoadStatus.Success.Source.MemoryCache,
                        nextKey = pagingLinkedHashMap.getPageNode(params.key)?.next?.key,
                        prevKey = pagingLinkedHashMap.getPageNode(params.key)?.prev?.key
                    )
                )
            } else if (pagingLinkedHashMap.isInDatabase(params)) {
                emit(PageLoadStatus.Loading.database())

                emit(
                    PageLoadStatus.Success(
                        snapshot = snapshot(),
                        isTerminal = true,
                        source = PageLoadStatus.Success.Source.Database,
                        nextKey = pagingLinkedHashMap.getPageNode(params.key)?.next?.key,
                        prevKey = pagingLinkedHashMap.getPageNode(params.key)?.prev?.key
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

    private suspend fun PageLoadStatusFlow<Id, Q, K, V, E>.handleSkipCacheLoad(
        params: PagingSource.LoadParams<K>
    ) {
        if (pagingLinkedHashMap.isInFlight(params.key)) {
            emit(PageLoadStatus.SkippingLoad.inFlight())
        } else {
            emit(PageLoadStatus.Loading.remote())

            handleFetchingFromRemote(params)
        }
    }

    private fun onSnapshot(snapshot: ItemSnapshotList<Id, Q, V>) {
        sideEffects.forEach { sideEffect -> sideEffect.invoke(snapshot) }
    }
}




