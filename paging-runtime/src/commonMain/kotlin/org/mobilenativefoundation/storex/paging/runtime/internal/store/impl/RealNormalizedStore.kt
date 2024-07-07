package org.mobilenativefoundation.storex.paging.runtime.internal.store.impl

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.ItemStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageLoadState
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.StoreInvalidation

internal class RealNormalizedStore<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val itemStore: ItemStore<Id, K, V>,
    private val pageStore: PageStore<Id, K, V>,
    private val storeInvalidation: StoreInvalidation
) : NormalizedStore<Id, K, V> {
    override suspend fun getItem(id: Id): V? {
        return itemStore.getItem(id)
    }

    override suspend fun saveItem(
        item: V
    ): PersistenceResult<Unit> {
        return itemStore.saveItem(item)
    }

    override suspend fun removeItem(id: Id): PersistenceResult<Unit> {
        return itemStore.removeItem(id)
    }

    override suspend fun clearAllItems(): PersistenceResult<Unit> {
        return itemStore.clearAllItems()
    }

    override suspend fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadState<Id, K, V>> {
        return pageStore.loadPage(params)
    }

    override suspend fun clearPage(key: K) {
        return pageStore.clearPage(key)
    }

    override suspend fun clearAllPages(): PersistenceResult<Unit> {
        return pageStore.clearAllPages()
    }

    override fun observeItem(id: Id): Flow<V?> {
        return itemStore.observeItem(id)
    }

    override suspend fun queryItems(predicate: (V) -> Boolean): PersistenceResult<List<V>> {
        return itemStore.queryItems(predicate)
    }

    override suspend fun invalidateAll(): PersistenceResult<Unit> {
        return storeInvalidation.invalidateAll()
    }

}