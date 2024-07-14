package org.mobilenativefoundation.storex.paging.runtime.internal.store.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier

/**
 * Represents a store for managing individual items.
 *
 * @param Id The type of the item identifier.
 * @param V The type of the item value, which must be Identifiable by Id.
 */
internal interface ItemStore<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    /**
     * Retrieves an item by its identifier.
     *
     * @param id The identifier of the item.
     * @return The item if found, null otherwise.
     */
    suspend fun getItem(id: Id): V?

    /**
     * Saves an item to both the memory cache and the persistent storage.
     *
     * @param item The item to save.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun saveItem(item: V): PersistenceResult<Unit>


    /**
     * Removes an item from both the memory cache and the persistent storage.
     *
     * @param id The identifier of the item to remove.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun removeItem(id: Id): PersistenceResult<Unit>

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
    suspend fun queryItems(predicate: (V) -> Boolean): PersistenceResult<List<V>>

    /**
     * Provides a flow of updates for a specific item.
     *
     * This method combines updates from both the memory cache and persistent storage.
     *
     * @param id The identifier of the item to observe.
     * @return A Flow emitting the latest state of the item, or null if the item is deleted.
     */
    fun observeItem(id: Id): Flow<V?>

}