package org.mobilenativefoundation.storex.paging.runtime.internal.store.impl

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.ItemStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageLoadState
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.StoreInvalidation

internal class RealNormalizedStore<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    private val itemStore: ItemStore<ItemId, PageRequestKey, ItemValue>,
    private val pageStore: PageStore<ItemId, PageRequestKey, ItemValue>,
    private val storeInvalidation: StoreInvalidation
) : NormalizedStore<ItemId, PageRequestKey, ItemValue> {
    override suspend fun getItem(id: ItemId): ItemValue? {
        return itemStore.getItem(id)
    }

    override suspend fun saveItem(
        item: ItemValue
    ): PersistenceResult<Unit> {
        return itemStore.saveItem(item)
    }

    override suspend fun removeItem(id: ItemId): PersistenceResult<Unit> {
        return itemStore.removeItem(id)
    }

    override suspend fun clearAllItems(): PersistenceResult<Unit> {
        return itemStore.clearAllItems()
    }

    override suspend fun loadPage(params: PagingSource.LoadParams<PageRequestKey>): Flow<PageLoadState<ItemId, PageRequestKey, ItemValue>> {
        return pageStore.loadPage(params)
    }

    override suspend fun clearPage(key: PageRequestKey) {
        return pageStore.clearPage(key)
    }

    override suspend fun clearAllPages(): PersistenceResult<Unit> {
        return pageStore.clearAllPages()
    }

    override fun observeItem(id: ItemId): Flow<ItemValue?> {
        return itemStore.observeItem(id)
    }

    override suspend fun queryItems(predicate: (ItemValue) -> Boolean): PersistenceResult<List<ItemValue>> {
        return itemStore.queryItems(predicate)
    }

    override suspend fun invalidateAll(): PersistenceResult<Unit> {
        return storeInvalidation.invalidateAll()
    }

}