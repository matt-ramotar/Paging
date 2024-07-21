package org.mobilenativefoundation.storex.paging.persistence.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.runtime.PagingSource

/**
 * Interface for persistence operations related to individual items.
 *
 * @param ItemId The type of the item identifier.
 * @param ItemValue The type of the item value.
 */
interface ItemPersistence<ItemId: Any, PageRequestKey: Any, ItemValue: Any> {
    /**
     * Retrieves an item by its identifier.
     *
     * @param id The identifier of the item to retrieve.
     * @return A PersistenceResult containing the item if found, or an error if not found or if an error occurred.
     */
    suspend fun getItem(id: ItemId): PersistenceResult<ItemValue?>

    /**
     * Provides a flow of updates for a specific item.
     *
     * @param id The identifier of the item to observe.
     * @return A Flow emitting the latest state of the item, or null if the item is deleted.
     */
    fun observeItem(id: ItemId): Flow<ItemValue?>

    /**
     * Saves or updates an item.
     *
     * @param item The item to save or update.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun saveItem(item: ItemValue, params: PagingSource.LoadParams<PageRequestKey>?): PersistenceResult<Unit>

    /**
     * Removes an item by its identifier.
     *
     * @param id The identifier of the item to remove.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun removeItem(id: ItemId): PersistenceResult<Unit>

    /**
     * Removes all items from the persistence layer.
     *
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun clearAllItems(): PersistenceResult<Unit>

    /**
     * Queries items based on a predicate.
     *
     * @param predicate A function that determines whether an item should be included in the result.
     * @return A PersistenceResult containing a list of items that match the predicate.
     */
    suspend fun queryItems(predicate: (ItemValue) -> Boolean): PersistenceResult<List<ItemValue>>
}