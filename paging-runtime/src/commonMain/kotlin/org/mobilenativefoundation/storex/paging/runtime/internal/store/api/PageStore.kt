package org.mobilenativefoundation.storex.paging.runtime.internal.store.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingSource

/**
 * Represents a store for managing pages of items.
 *
 * @param Id The type of the item identifier.
 * @param K The type of the paging key.
 * @param V The type of the item value, which must be Identifiable by Id.
 */
internal interface PageStore<Id : Identifier<Id>, K : Any, V : Identifiable<Id>> {
    /**
     * Loads a page of data based on the given load parameters.
     *
     * @param params The load parameters for the page.
     * @return A Flow emitting the PageLoadState for the requested page.
     */
    suspend fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadState<Id, K, V>>


    /**
     * Clears the page associated with the given key.
     *
     * @param key The key of the page to clear.
     */
    suspend fun clearPage(key: K)

}