package org.mobilenativefoundation.storex.paging.persistence.api

import org.mobilenativefoundation.storex.paging.runtime.PagingSource


/**
 * Interface for persistence operations related to pages of items.
 *
 * @param ItemId The type of the item identifier.
 * @param PageRequestKey The type of the paging key.
 * @param ItemValue The type of the item value.
 */
interface PagePersistence<ItemId: Any, PageRequestKey: Any, ItemValue: Any> {
    /**
     * Checks if a page exists for the given load parameters.
     *
     * @param params The load parameters of the page to check.
     * @return A PersistenceResult containing a boolean indicating whether the page exists.
     */
    suspend fun exists(params: PagingSource.LoadParams<PageRequestKey>): PersistenceResult<Boolean>

    /**
     * Saves a page of data.
     *
     * @param params The load parameters associated with the page.
     * @param data The page data to save.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun savePage(
        params: PagingSource.LoadParams<PageRequestKey>,
        data: PagingSource.LoadResult.Data<ItemId, PageRequestKey, ItemValue>
    ): PersistenceResult<Unit>

    /**
     * Retrieves a page of data for the given load parameters.
     *
     * @param params The load parameters of the page to retrieve.
     * @return A PersistenceResult containing the page data if found, or an error if not found or if an error occurred.
     */
    suspend fun getPage(params: PagingSource.LoadParams<PageRequestKey>): PersistenceResult<PagingSource.LoadResult.Data<ItemId, PageRequestKey, ItemValue>?>

    /**
     * Removes a page of data for the given load parameters.
     *
     * @param params The load parameters of the page to remove.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun removePage(params: PagingSource.LoadParams<PageRequestKey>): PersistenceResult<Unit>

    /**
     * Removes all pages from the persistence layer.
     *
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun clearAllPages(): PersistenceResult<Unit>
}