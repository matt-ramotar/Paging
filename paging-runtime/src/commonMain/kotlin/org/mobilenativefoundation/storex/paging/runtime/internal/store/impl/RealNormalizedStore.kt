package org.mobilenativefoundation.storex.paging.runtime.internal.store.impl

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.persistence.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingSource
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.ItemStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageLoadState
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.StoreInvalidation

internal class RealNormalizedStore<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val itemStore: ItemStore<Id, V>,
    private val pageStore: PageStore<Id, K, V>,
    private val storeInvalidation: StoreInvalidation
) : NormalizedStore<Id, K, V> {
    override suspend fun getItem(id: Id): V? {
        return itemStore.getItem(id)
    }

    override fun getUpdatingItem(id: Id): UpdatingItem<Id, V> {
        return itemStore.getUpdatingItem(id)
    }

    override suspend fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadState<Id, K, V>> {
        return pageStore.loadPage(params)
    }

    override suspend fun clearPage(key: K) {
        return pageStore.clearPage(key)
    }

    override suspend fun invalidateAll(): PersistenceResult<Unit> {
        return storeInvalidation.invalidateAll()
    }

}