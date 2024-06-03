package org.mobilenativefoundation.storex.paging.internal.impl


import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.db.DriverFactory
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.api.NormalizedStore

class ConcurrentNormalizedStore<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    private val pageFetcher: Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, Q, K, V, E>>,
    private val registry: KClassRegistry<Id,Q, K, V, E>,
    private val errorFactory: ErrorFactory<E>,
    private val itemFetcher: Fetcher<Id, V>?,
    driverFactory: DriverFactory?,
    private val maxSize: Int = 500,
    private val fetchingStateHolder: FetchingStateHolder<Id,Q, K>,
    private val sideEffects: List<SideEffect<Id, Q, V>>,
    private val pagingConfig: PagingConfig<Id, Q, K>
) : NormalizedStore<Id, Q, K, V, E> {

    private val db = driverFactory?.let { PagingDb(driverFactory.createDriver()) }
    private val itemCache = ItemCache<Id,Q,  V>()
    private val pageCache = PageCache(maxSize, pagingConfig, itemCache)
    private val pageLoadStatusProvider = PageLoadStatusProvider(
        pageFetcher,
        registry,
        errorFactory,
        pageCache,
        itemCache,
        db,
        pagingConfig,
        sideEffects
    )
    private val selfUpdatingItemPresenter = SelfUpdatingItemPresenter(
        itemFetcher,
        registry,
        errorFactory,
        itemCache,
        db,
        fetchingStateHolder
    )

    override fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadStatus<Id, Q, K, V, E>> =
        pageLoadStatusProvider.loadPage(params)

    override fun selfUpdatingItem(id: Q): SelfUpdatingItem<Id, Q, V, E> =
        selfUpdatingItemPresenter.present(id)

    override fun selfUpdatingPage(key: K): SelfUpdatingPage<Id, Q, K, V, E> {
        TODO()
    }

    override fun getItem(id: Q): V? {
        TODO("Not yet implemented")
    }

    override fun invalidate() {
        pageCache.clear()
        itemCache.clear()
        db?.let {
            it.itemQueries.removeAllItems()
            it.pageQueries.removeAllPages()
        }
    }

    override fun clear(key: K) {
        pageCache.removePage(key)
    }
}