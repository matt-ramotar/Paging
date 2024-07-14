package org.mobilenativefoundation.storex.paging.persistence.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingSource

/**
 * Interface for persistence operations related to individual items.
 *
 * @param Id The type of the item identifier.
 * @param V The type of the item value.
 */
interface ItemPersistence<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    /**
     * Retrieves an item by its identifier.
     *
     * @param id The identifier of the item to retrieve.
     * @return A PersistenceResult containing the item if found, or an error if not found or if an error occurred.
     */
    suspend fun getItem(id: Id): PersistenceResult<V?>

    /**
     * Provides a flow of updates for a specific item.
     *
     * @param id The identifier of the item to observe.
     * @return A Flow emitting the latest state of the item, or null if the item is deleted.
     */
    fun observeItem(id: Id): Flow<V?>

    /**
     * Saves or updates an item.
     *
     * @param item The item to save or update.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun saveItem(item: V, params: PagingSource.LoadParams<K>?): PersistenceResult<Unit>

    /**
     * Removes an item by its identifier.
     *
     * @param id The identifier of the item to remove.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    suspend fun removeItem(id: Id): PersistenceResult<Unit>

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
    suspend fun queryItems(predicate: (V) -> Boolean): PersistenceResult<List<V>>
}