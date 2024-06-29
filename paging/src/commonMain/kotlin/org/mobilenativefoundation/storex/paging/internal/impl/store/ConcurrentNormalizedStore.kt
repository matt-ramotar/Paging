package org.mobilenativefoundation.storex.paging.internal.impl.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.InternalSerializationApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.internal.impl.KClassRegistry
import org.mobilenativefoundation.storex.paging.internal.impl.PageLoadState
import org.mobilenativefoundation.storex.paging.scope.Database

typealias PageLoadStatusFlow<Id, K, V> = FlowCollector<PageLoadState<Id, K, V>>

typealias WithDb = ((suspend (PagingDb) -> Unit) -> Unit)


@OptIn(InternalSerializationApi::class)
class ConcurrentNormalizedStore<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val pageFetcher: Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V>>,
    private val db: Database<Id, K, V>? = null,
    private val fetchingStateHolder: FetchingStateHolder<Id, K>,
    private val sideEffects: List<SideEffect<Id, V>>,
    private val pagingConfig: PagingConfig<Id, K>,
    private val selfUpdatingItemPresenter: SelfUpdatingItemPresenter<Id, K, V>,
    private val linkedHashMapManager: LinkedHashMapManager<Id, K, V>,
) : NormalizedStore<Id, K, V> {

    override fun selfUpdatingItem(id: Id): SelfUpdatingItem<Id, V> {
        return SelfUpdatingItem { events -> selfUpdatingItemPresenter.present(id, events) }
    }

    override fun selfUpdatingPage(key: K): SelfUpdatingPage<Id, V> {
        TODO("Not yet implemented")
    }

    override fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadState<Id, K, V>> = flow {

        // If needed, add placeholders, before loading
        if (shouldAddPlaceholders(params)) {
            when (params.direction) {
                LoadDirection.Prepend -> linkedHashMapManager.prependPlaceholders(params)
                LoadDirection.Append -> linkedHashMapManager.appendPlaceholders(params)
            }
        }

        // Emit processing status, before loading
        emit(PageLoadState.Processing())

        handleLoading(params)
    }

    override fun getItem(id: Id): V? {
        return linkedHashMapManager.getItem(id) ?: getItemFromDb(id)
    }

    private fun getItemFromDb(id: Id): V? {
        return db?.itemQueries?.getItem(id)
    }


    override fun clear(key: K) {
        linkedHashMapManager.removePage(key)
    }

    override fun invalidate() {
        linkedHashMapManager.invalidate()
    }

    private fun shouldAddPlaceholders(params: PagingSource.LoadParams<K>): Boolean {
        val placeholdersAreEnabled = pagingConfig.placeholderId != null
        val mightFetchFromRemote = params.strategy != LoadStrategy.LocalOnly
        return placeholdersAreEnabled && mightFetchFromRemote
    }


    private suspend fun PageLoadStatusFlow<Id, K, V>.handleLoading(
        params: PagingSource.LoadParams<K>
    ) {
        when (params.strategy) {
            is LoadStrategy.CacheFirst -> handleCacheFirstLoad(params)
            LoadStrategy.LocalOnly -> handleLocalOnlyLoad(params)
            LoadStrategy.SkipCache -> handleSkipCacheLoad(params)
        }
    }

    private suspend fun PageLoadStatusFlow<Id, K, V>.handleCacheFirstLoad(
        params: PagingSource.LoadParams<K>
    ) {
        if (linkedHashMapManager.isInFlight(params.key)) {
            emit(PageLoadState.SkippingLoad.inFlight())
        } else {
            if (linkedHashMapManager.isCached(params.key)) {
                emit(PageLoadState.Loading.memoryCache())

                emit(
                    PageLoadState.Success(
                        snapshot = snapshot(),
                        isTerminal = true,
                        source = PageLoadState.Success.Source.MemoryCache,
                        nextKey = linkedHashMapManager.getPageNode(params.key)?.next?.key,
                        prevKey = linkedHashMapManager.getPageNode(params.key)?.prev?.key
                    )
                )
            } else if (linkedHashMapManager.isInDatabase(params)) {
                emit(PageLoadState.Loading.database())

                emit(
                    PageLoadState.Success(
                        snapshot = snapshot(),
                        isTerminal = true,
                        source = PageLoadState.Success.Source.Database,
                        nextKey = linkedHashMapManager.getPageNode(params.key)?.next?.key,
                        prevKey = linkedHashMapManager.getPageNode(params.key)?.prev?.key
                    )
                )
            }

            // Either way, we want to fetch from the remote data source
            handleFetchingFromRemote(params)
        }
    }

    private suspend fun PageLoadStatusFlow<Id, K, V>.handleFetchingFromRemote(
        params: PagingSource.LoadParams<K>
    ) {
        emit(PageLoadState.Loading.remote())

        linkedHashMapManager.getPageNode(params.key)?.isInFlight = true

        return when (val fetcherResult = pageFetcher.invoke(params).first()) {
            is FetcherResult.Data -> handleFetcherResultData(params, fetcherResult)
            is FetcherResult.Error.Custom<*> -> handleCustomError(fetcherResult)
            is FetcherResult.Error.Exception -> handleException(fetcherResult)
            is FetcherResult.Error.Message -> handleErrorMessage(fetcherResult)
        }

    }

    private suspend fun PageLoadStatusFlow<Id, K, V>.handleCustomError(fetcherResult: FetcherResult.Error.Custom<*>) {
        emit(PageLoadState.Error.Exception(Throwable(fetcherResult.error.toString()), isTerminal = true))
    }

    private suspend fun PageLoadStatusFlow<Id, K, V>.handleException(fetcherResult: FetcherResult.Error.Exception) {
        emit(PageLoadState.Error.Exception(fetcherResult.error, isTerminal = true))
    }

    private suspend fun PageLoadStatusFlow<Id, K, V>.handleErrorMessage(fetcherResult: FetcherResult.Error.Message) {
        emit(PageLoadState.Error.Message(fetcherResult.message, isTerminal = true))
    }

    private suspend fun PageLoadStatusFlow<Id, K, V>.handleFetcherResultData(
        params: PagingSource.LoadParams<K>,
        fetcherResult: FetcherResult.Data<PagingSource.LoadResult.Data<Id, K, V>>
    ) {
        if (fetcherResult.value.items.isEmpty()) {
            emit(PageLoadState.Empty(isTerminal = true, PageLoadState.Empty.Reason.NetworkResponse))
        } else {
            var maxItemLoaded = fetcherResult.value.items[0].id
            var minItemLoaded = fetcherResult.value.items[0].id

            when (params.direction) {
                LoadDirection.Prepend -> {
                    fetcherResult.value.items.asReversed().forEach { item ->
                        linkedHashMapManager.saveItem(item, params)

                        if (item.id < minItemLoaded) {
                            minItemLoaded = item.id
                        }

                        if (item.id > maxItemLoaded) {
                            maxItemLoaded = item.id
                        }
                    }

                    linkedHashMapManager.prependPage(params, fetcherResult)
                }

                LoadDirection.Append -> {
                    fetcherResult.value.items.forEach { item ->
                        linkedHashMapManager.saveItem(item, params)

                        if (item.id < minItemLoaded) {
                            minItemLoaded = item.id
                        }

                        if (item.id > maxItemLoaded) {
                            maxItemLoaded = item.id
                        }
                    }

                    linkedHashMapManager.appendPage(params, fetcherResult)
                }
            }

            fetchingStateHolder.updateMaxItemLoadedSoFar(maxItemLoaded)
            fetchingStateHolder.updateMinItemLoadedSoFar(minItemLoaded)

            emit(
                PageLoadState.Success(
                    snapshot = snapshot(),
                    isTerminal = true,
                    source = PageLoadState.Success.Source.Network,
                    nextKey = fetcherResult.value.nextKey,
                    prevKey = fetcherResult.value.prevKey ?: linkedHashMapManager.getPageNode(params.key)?.prev?.key
                )
            )
        }
    }

    private fun snapshot(): ItemSnapshotList<Id, V> {
        return ItemSnapshotList(linkedHashMapManager.getItemsInOrder()).also {
            onSnapshot(it)
        }
    }

    private suspend fun PageLoadStatusFlow<Id, K, V>.handleLocalOnlyLoad(
        params: PagingSource.LoadParams<K>
    ) {
        if (linkedHashMapManager.isInFlight(params.key)) {
            emit(PageLoadState.SkippingLoad.inFlight())
        } else {
            if (linkedHashMapManager.isCached(params.key)) {
                emit(
                    PageLoadState.Success(
                        snapshot = snapshot(),
                        isTerminal = true,
                        source = PageLoadState.Success.Source.MemoryCache,
                        nextKey = linkedHashMapManager.getPageNode(params.key)?.next?.key,
                        prevKey = linkedHashMapManager.getPageNode(params.key)?.prev?.key
                    )
                )
            } else if (linkedHashMapManager.isInDatabase(params)) {
                emit(PageLoadState.Loading.database())

                emit(
                    PageLoadState.Success(
                        snapshot = snapshot(),
                        isTerminal = true,
                        source = PageLoadState.Success.Source.Database,
                        nextKey = linkedHashMapManager.getPageNode(params.key)?.next?.key,
                        prevKey = linkedHashMapManager.getPageNode(params.key)?.prev?.key
                    )
                )
            } else {
                emit(
                    PageLoadState.Empty(
                        true,
                        PageLoadState.Empty.Reason.LocalOnlyRequest
                    )
                )
            }
        }

    }

    private suspend fun PageLoadStatusFlow<Id, K, V>.handleSkipCacheLoad(
        params: PagingSource.LoadParams<K>
    ) {
        if (linkedHashMapManager.isInFlight(params.key)) {
            emit(PageLoadState.SkippingLoad.inFlight())
        } else {
            emit(PageLoadState.Loading.remote())

            handleFetchingFromRemote(params)
        }
    }

    private fun onSnapshot(snapshot: ItemSnapshotList<Id, V>) {
        sideEffects.forEach { sideEffect -> sideEffect.invoke(snapshot) }
    }
}