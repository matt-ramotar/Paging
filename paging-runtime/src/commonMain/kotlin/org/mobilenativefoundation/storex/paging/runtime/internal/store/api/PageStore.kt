package org.mobilenativefoundation.storex.paging.runtime.internal.store.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.PagingSource

/**
 * Represents a store for managing pages of items.
 *
 * @param ItemId The type of the item identifier.
 * @param PageRequestKey The type of the paging key.
 * @param ItemValue The type of the item value.
 */
internal interface PageStore<ItemId : Any, PageRequestKey : Any, ItemValue : Any> {
    /**
     * Loads a page of data based on the given load parameters.
     *
     * @param params The load parameters for the page.
     * @return A Flow emitting the PageLoadState for the requested page.
     */
    suspend fun loadPage(params: PagingSource.LoadParams<PageRequestKey>): Flow<PageLoadState<ItemId, PageRequestKey, ItemValue>>


    /**
     * Clears the page associated with the given key.
     *
     * @param key The key of the page to clear.
     */
    suspend fun clearPage(key: PageRequestKey)

    /**
     * Clears all pages from the memory cache and the persistent storage.
     *
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun clearAllPages(): PersistenceResult<Unit>
}