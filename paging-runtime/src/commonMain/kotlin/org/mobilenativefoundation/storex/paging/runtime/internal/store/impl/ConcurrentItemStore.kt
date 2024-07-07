package org.mobilenativefoundation.storex.paging.runtime.internal.store.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import org.mobilenativefoundation.storex.paging.persistence.api.ItemPersistence
import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LinkedHashMapManager
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.ItemStore

/**
 * A thread-safe implementation of ItemStore that manages individual items.
 *
 * This class uses a combination of in-memory cache and a persistent storage layer to provide
 * efficient and reliable item management. It ensures thread-safety through the use
 * of a Mutex for all operations that modify shared state.
 *
 * @param Id The type of the item identifier.
 * @param V The type of the item value.
 * @property itemMemoryCache In-memory cache for quick access to items.
 * @property itemPersistence Persistent storage layer for items.
 */
internal class ConcurrentItemStore<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val itemMemoryCache: MutableMap<Id, V>,
    private val itemPersistence: ItemPersistence<Id, K, V>,
    private val linkedHashMapManager: LinkedHashMapManager<Id, K, V>
) : ItemStore<Id, K, V> {

    // Mutex for ensuring thread-safe access to shared resources
    private val mutex = Mutex()


    override suspend fun getItem(id: Id): V? {
        return linkedHashMapManager.getItem(id)
    }

    /**
     * Saves an item to both the memory cache and the persistent storage.
     *
     * @param item The item to save.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    override suspend fun saveItem(
        item: V
    ): PersistenceResult<Unit> {
        val result = linkedHashMapManager.saveItem(item)
        // TODO(): Update network?
        return result
    }

    /**
     * Removes an item from both the memory cache and the persistent storage.
     *
     * @param id The identifier of the item to remove.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    override suspend fun removeItem(id: Id): PersistenceResult<Unit> {
        return linkedHashMapManager.removeItem(id)
    }

    /**
     * Clears all items from the memory cache and the persistent storage.
     *
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    override suspend fun clearAllItems(): PersistenceResult<Unit> {
        return linkedHashMapManager.removeAllItems()
    }

    /**
     * Queries items based on a predicate.
     *
     * This method combines results from both the memory cache and persistent storage.
     *
     * @param predicate A function that determines whether an item should be included in the result.
     * @return A PersistenceResult containing a list of items that match the predicate.
     */
    override suspend fun queryItems(predicate: (V) -> Boolean): PersistenceResult<List<V>> {
        return linkedHashMapManager.queryItems(predicate)
    }

    /**
     * Provides a flow of updates for a specific item.
     *
     * This method combines updates from both the memory cache and persistent storage.
     *
     * @param id The identifier of the item to observe.
     * @return A Flow emitting the latest state of the item, or null if the item is deleted.
     */
    override fun observeItem(id: Id): Flow<V?> {
        return linkedHashMapManager.observeItem(id)
    }
}