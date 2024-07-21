package org.mobilenativefoundation.storex.paging.runtime.internal.store.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult

/**
 * Represents a store for managing individual items.
 *
 * @param ItemId The type of the item identifier.
 * @param ItemValue The type of the item value.
 */
internal interface ItemStore<ItemId : Any, PageRequestKey : Any, ItemValue : Any> {
    /**
     * Retrieves an item by its identifier.
     *
     * @param id The identifier of the item.
     * @return The item if found, null otherwise.
     */
    suspend fun getItem(id: ItemId): ItemValue?

    /**
     * Saves an item to both the memory cache and the persistent storage.
     *
     * @param item The item to save.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun saveItem(item: ItemValue): PersistenceResult<Unit>


    /**
     * Removes an item from both the memory cache and the persistent storage.
     *
     * @param id The identifier of the item to remove.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun removeItem(id: ItemId): PersistenceResult<Unit>

    /**
     * Clears all items from the memory cache and the persistent storage.
     *
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun clearAllItems(): PersistenceResult<Unit>

    /**
     * Queries items based on a predicate.
     *
     * This method combines results from both the memory cache and persistent storage.
     *
     * @param predicate A function that determines whether an item should be included in the result.
     * @return A PersistenceResult containing a list of items that match the predicate.
     */
    suspend fun queryItems(predicate: (ItemValue) -> Boolean): PersistenceResult<List<ItemValue>>

    /**
     * Provides a flow of updates for a specific item.
     *
     * This method combines updates from both the memory cache and persistent storage.
     *
     * @param id The identifier of the item to observe.
     * @return A Flow emitting the latest state of the item, or null if the item is deleted.
     */
    fun observeItem(id: ItemId): Flow<ItemValue?>

}