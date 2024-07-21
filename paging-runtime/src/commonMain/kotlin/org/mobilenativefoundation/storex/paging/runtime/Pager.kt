package org.mobilenativefoundation.storex.paging.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.custom.LaunchEffect
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.persistence.api.ItemPersistence
import org.mobilenativefoundation.storex.paging.persistence.api.PagePersistence
import org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.impl.PagerBuilder

interface Pager<ItemId : Any, PageRequestKey : Any, ItemValue : Any> {
    val state: StateFlow<PagingState<ItemId, PageRequestKey, ItemValue>>
    suspend fun dispatch(action: Action<ItemId, PageRequestKey, ItemValue>)
    suspend fun getUpdatingItem(id: ItemId): UpdatingItem<ItemId, ItemValue>

    interface Builder<ItemId : Any, PageRequestKey : Any, ItemValue : Any> {
        fun setInitialState(state: PagingState<ItemId, PageRequestKey, ItemValue>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setInitialLoadParams(params: PagingSource.LoadParams<PageRequestKey>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setPagingSource(source: PagingSource<ItemId, PageRequestKey, ItemValue>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setCoroutineDispatcher(dispatcher: CoroutineDispatcher): Builder<ItemId, PageRequestKey, ItemValue>
        fun addLaunchEffect(effect: LaunchEffect): Builder<ItemId, PageRequestKey, ItemValue>
        fun addSideEffect(effect: SideEffect<ItemId, ItemValue>): Builder<ItemId, PageRequestKey, ItemValue>
        fun addMiddleware(mw: Middleware<PageRequestKey>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setInitialFetchingState(state: FetchingState<ItemId, PageRequestKey>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setFetchingStrategy(strategy: FetchingStrategy<ItemId, PageRequestKey, ItemValue>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setErrorHandlingStrategy(strategy: ErrorHandlingStrategy): Builder<ItemId, PageRequestKey, ItemValue>
        fun setItemMemoryCache(cache: MutableMap<ItemId, ItemValue>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setPageMemoryCache(cache: MutableMap<PageRequestKey, PagingSource.LoadResult.Data<ItemId, PageRequestKey, ItemValue>>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setItemPersistence(persistence: ItemPersistence<ItemId, PageRequestKey, ItemValue>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setPagePersistence(persistence: PagePersistence<ItemId, PageRequestKey, ItemValue>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setItemUpdater(updater: Updater<ItemId, ItemValue, *>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setPlaceholderFactory(placeholderFactory: PlaceholderFactory<ItemId, PageRequestKey, ItemValue>): Builder<ItemId, PageRequestKey, ItemValue>
        fun setInitialOperations(operations: List<Operation<ItemId, PageRequestKey, ItemValue>>): Builder<ItemId, PageRequestKey, ItemValue>
        fun build(): Pager<ItemId, PageRequestKey, ItemValue>
    }

    companion object {
        fun <Id : Any, K : Any, V : Any> builder(
            pagingConfig: PagingConfig<Id, K>
        ): Builder<Id, K, V> = PagerBuilder(pagingConfig)
    }

}
