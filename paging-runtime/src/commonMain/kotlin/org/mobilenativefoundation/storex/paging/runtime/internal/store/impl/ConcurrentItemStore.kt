package org.mobilenativefoundation.storex.paging.runtime.internal.store.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.persistence.ItemPersistence
import org.mobilenativefoundation.storex.paging.persistence.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
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
) : ItemStore<Id, V> {

    // Mutex for ensuring thread-safe access to shared resources
    private val mutex = Mutex()

    /**
     * Retrieves an item by its identifier.
     *
     * This method first checks the in-memory cache, and if the item is not found,
     * it attempts to retrieve it from the persistent storage.
     *
     * @param id The identifier of the item to retrieve.
     * @return The item if found, null otherwise.
     */
    override suspend fun getItem(id: Id): V? = mutex.withLock {
        itemMemoryCache[id] ?: when (val result = itemPersistence.getItem(id)) {
            is PersistenceResult.Success -> result.data?.also { item ->
                // If found in persistent storage, update the memory cache
                itemMemoryCache[id] = item
            }

            is PersistenceResult.Error -> {
                // Log the error or handle it as appropriate for your application
                println("Error retrieving item: ${result.message}")
                null
            }
        }
    }

    /**
     * Saves an item to both the memory cache and the persistent storage.
     *
     * @param item The item to save.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    override suspend fun saveItem(item: V): PersistenceResult<Unit> = mutex.withLock {
        itemMemoryCache[item.id] = item
        itemPersistence.saveItem(item)
    }

    /**
     * Removes an item from both the memory cache and the persistent storage.
     *
     * @param id The identifier of the item to remove.
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    override suspend fun removeItem(id: Id): PersistenceResult<Unit> = mutex.withLock {
        itemMemoryCache.remove(id)
        itemPersistence.removeItem(id)
    }

    /**
     * Clears all items from the memory cache and the persistent storage.
     *
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    override suspend fun clearAllItems(): PersistenceResult<Unit> = mutex.withLock {
        itemMemoryCache.clear()
        itemPersistence.clearAllItems()
    }

    /**
     * Queries items based on a predicate.
     *
     * This method combines results from both the memory cache and persistent storage.
     *
     * @param predicate A function that determines whether an item should be included in the result.
     * @return A PersistenceResult containing a list of items that match the predicate.
     */
    override suspend fun queryItems(predicate: (V) -> Boolean): PersistenceResult<List<V>> =
        mutex.withLock {
            val memoryItems = itemMemoryCache.values.filter(predicate)
            when (val persistenceResult = itemPersistence.queryItems(predicate)) {
                is PersistenceResult.Success -> {
                    val persistenceItems = persistenceResult.data
                    val combinedItems =
                        (memoryItems + persistenceItems).distinctBy { it.id }
                    PersistenceResult.Success(combinedItems)
                }

                is PersistenceResult.Error -> persistenceResult
            }
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
        return itemPersistence.observeItem(id).map { persistentItem ->
            mutex.withLock {
                itemMemoryCache[id] ?: persistentItem?.also { itemMemoryCache[id] = it }
            }
        }
    }
}