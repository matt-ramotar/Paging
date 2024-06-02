package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.coroutines.flow.Flow
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

@Suppress("UNCHECKED_CAST")
@OptIn(InternalSerializationApi::class)
class PageLoadStatusProvider<Id : Comparable<Id>, Q: Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    private val pageFetcher: Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, Q, K, V, E>>,
    private val registry: KClassRegistry<Id, Q, K, V, E>,
    private val errorFactory: ErrorFactory<E>,
    private val pageCache: PageCache<Id, Q, K, V>,
    private val itemCache: ItemCache<Id, Q, V>,
    private val db: PagingDb?,
    private val pagingConfig: PagingConfig<Id, Q, K>,
    private val sideEffects: List<SideEffect<Id, Q, V>>,
) {
    fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadStatus<Id, Q, K, V, E>> = flow {
        emit(PageLoadStatus.Processing())

        when (params.strategy) {
            is LoadStrategy.CacheFirst -> {
                if (pageCache.getPage(params.key) != null) {
                    emit(PageLoadStatus.Loading.memoryCache())
                    emit(successStatus(params.key))
                } else if (isPageInDatabase(params)) {
                    emit(PageLoadStatus.Loading.database())
                    emit(successStatus(params.key))
                } else {
                    emit(PageLoadStatus.Loading.remote())
                    emit(fetchFromNetwork(params))
                }
            }

            LoadStrategy.SkipCache -> {
                emit(PageLoadStatus.Loading.remote())
                emit(fetchFromNetwork(params))
            }

            LoadStrategy.LocalOnly -> {
                if (pageCache.getPage(params.key) != null) {
                    emit(successStatus(params.key))
                } else if (isPageInDatabase(params)) {
                    emit(PageLoadStatus.Loading.database())
                    emit(successStatus(params.key))
                } else {
                    emit(PageLoadStatus.Empty(true, PageLoadStatus.Empty.Reason.LocalOnlyRequest))
                }
            }

            LoadStrategy.Refresh -> {
                // TODO: Implement refresh strategy
            }
        }
    }

    private suspend fun fetchFromNetwork(params: PagingSource.LoadParams<K>): PageLoadStatus<Id,Q, K, V, E> {
        return when (val fetcherResult = pageFetcher.invoke(params).first()) {
            is FetcherResult.Data -> {
                val items = fetcherResult.value.items
                when (params.direction) {
                    LoadDirection.Prepend -> {
                        items.asReversed().forEach { item ->
                            itemCache.updateItem(item)
                        }
                        pageCache.addPage(params.key, items.map { it.id })
                    }

                    LoadDirection.Append -> {
                        items.forEach { item ->
                            itemCache.updateItem(item)
                        }
                        pageCache.addPage(params.key, items.map { it.id })
                    }
                }
                successStatus(params.key)
            }

            is FetcherResult.Error.Exception -> {
                if (fetcherResult.error is PagingError) {
                    val pagingError = fetcherResult.error as PagingError
                    val error = Json.decodeFromString(registry.error.serializer(), pagingError.encodedError)
                    PageLoadStatus.Error(error, pagingError.extras, true)
                } else {
                    val error = errorFactory.create(fetcherResult.error)
                    PageLoadStatus.Error(error, fetcherResult.error.extras(), true)
                }
            }

            is FetcherResult.Error.Custom<*> -> {
                val error = fetcherResult.error as E
                PageLoadStatus.Error(error, null, true)
            }

            is FetcherResult.Error.Message -> {
                val error = errorFactory.create(fetcherResult.message)
                PageLoadStatus.Error(error, null, true)
            }
        }
    }

    private fun successStatus(key: K): PageLoadStatus.Success<Id, Q, K, V, E> {
        return PageLoadStatus.Success(
            snapshot = snapshot(),
            isTerminal = true,
            source = PageLoadStatus.Success.Source.Network,
            nextKey = pageCache.getPageNode(key)?.next?.key,
            prevKey = pageCache.getPageNode(key)?.prev?.key
        )
    }

    private fun isPageInDatabase(params: PagingSource.LoadParams<K>): Boolean {
        val localParams = Json.encodeToString(
            PagingSource.LoadParams.serializer(registry.key.serializer()),
            params
        )
        return db?.pageQueries?.getPage(localParams)?.executeAsOneOrNull() != null
    }

    private fun snapshot(): ItemSnapshotList<Id,Q, V> {
        return ItemSnapshotList(
            items = pageCache.getItemsInOrder()
        ).also { onSnapshot(it) }
    }

    private fun onSnapshot(snapshot: ItemSnapshotList<Id,Q, V>) {
        sideEffects.forEach { it.invoke(snapshot) }
    }
}