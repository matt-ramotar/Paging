package org.mobilenativefoundation.storex.paging.runtime

import kotlinx.coroutines.CoroutineDispatcher
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.custom.LaunchEffect
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.custom.SideEffect
import org.mobilenativefoundation.storex.paging.persistence.api.ItemPersistence
import org.mobilenativefoundation.storex.paging.persistence.api.PagePersistence
import org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.impl.PagingScopeBuilder

interface PagingScope<ItemId: Any, PageRequestKey: Any, ItemValue: Any> {
    fun getPager(): Pager<ItemId>
    fun getOperationManager(): OperationManager<ItemId, PageRequestKey, ItemValue>
    fun getDispatcher(): Dispatcher<PageRequestKey>
    fun getUpdatingItemProvider(): UpdatingItemProvider<ItemId, ItemValue>

    interface Builder<Id: Any, K: Any, V: Any> {
        fun setInitialState(state: PagingState<Id>): Builder<Id, K, V>
        fun setInitialLoadParams(params: PagingSource.LoadParams<K>): Builder<Id, K, V>
        fun setPagingSource(source: PagingSource<Id, K, V>): Builder<Id, K, V>
        fun setCoroutineDispatcher(dispatcher: CoroutineDispatcher): Builder<Id, K, V>
        fun addLaunchEffect(effect: LaunchEffect): Builder<Id, K, V>
        fun addSideEffect(effect: SideEffect<Id, V>): Builder<Id, K, V>
        fun addMiddleware(mw: Middleware<K>): Builder<Id, K, V>
        fun setInitialFetchingState(state: FetchingState<Id, K>): Builder<Id, K, V>
        fun setFetchingStrategy(strategy: FetchingStrategy<Id, K>): Builder<Id, K, V>
        fun setErrorHandlingStrategy(strategy: ErrorHandlingStrategy): Builder<Id, K, V>
        fun setItemMemoryCache(cache: MutableMap<Id, V>): Builder<Id, K, V>
        fun setPageMemoryCache(cache: MutableMap<K, PagingSource.LoadResult.Data<Id, K, V>>): Builder<Id, K, V>
        fun setItemPersistence(persistence: ItemPersistence<Id, K, V>): Builder<Id, K, V>
        fun setPagePersistence(persistence: PagePersistence<Id, K, V>): Builder<Id, K, V>
        fun setItemUpdater(updater: Updater<Id, V, *>): Builder<Id, K, V>
        fun setPlaceholderFactory(placeholderFactory: PlaceholderFactory<Id, K, V>): Builder<Id, K, V>
        fun build(): PagingScope<Id, K, V>
    }

    companion object {
        fun <Id: Any, K: Any, V: Any> builder(
            pagingConfig: PagingConfig<Id, K>
        ): Builder<Id, K, V> = PagingScopeBuilder(pagingConfig)
    }
}