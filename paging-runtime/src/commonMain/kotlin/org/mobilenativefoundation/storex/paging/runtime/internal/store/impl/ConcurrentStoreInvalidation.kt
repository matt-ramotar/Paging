package org.mobilenativefoundation.storex.paging.runtime.internal.store.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.persistence.PersistenceResult
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.StoreInvalidation

/**
 * A thread-safe implementation of StoreInvalidation that manages the invalidation of stored data.
 *
 * This class provides mechanisms to invalidate all data in the store, as well as to observe
 * the invalidation state. It ensures thread-safety through the use of a Mutex for all operations
 * that modify shared state.
 *
 * @param itemStore The store managing individual items.
 * @param pageStore The store managing pages of items.
 */
internal class ConcurrentStoreInvalidation<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val itemStore: ConcurrentItemStore<Id, K, V>,
    private val pageStore: ConcurrentPageStore<Id, K, V>
) : StoreInvalidation {

    // Mutex for ensuring thread-safe access to shared resources
    private val mutex = Mutex()

    // StateFlow to represent the current invalidation state
    private val _invalidationState = MutableStateFlow(InvalidationState.VALID)
    val invalidationState: StateFlow<InvalidationState> = _invalidationState.asStateFlow()

    /**
     * Invalidates all data in the store.
     *
     * This method clears all items and pages from both the in-memory cache and persistent storage.
     * It also updates the invalidation state to trigger observers.
     *
     * @return A PersistenceResult indicating success or failure of the operation.
     */
    override suspend fun invalidateAll(): PersistenceResult<Unit> = mutex.withLock {
        try {
            // Set the state to invalidating
            _invalidationState.value = InvalidationState.INVALIDATING

            // Clear all items
            val itemClearResult = itemStore.clearAllItems()
            if (itemClearResult is PersistenceResult.Error) {
                return PersistenceResult.Error("Failed to clear items: ${itemClearResult.message}")
            }

            // Clear all pages
            pageStore.clearAllPages()

            // Set the state to invalid
            _invalidationState.value = InvalidationState.INVALID

            return PersistenceResult.Success(Unit)
        } catch (e: Exception) {
            // If any unexpected error occurs, set the state to error and return the error
            _invalidationState.value = InvalidationState.ERROR
            return PersistenceResult.Error("Unexpected error during invalidation: ${e.message}")
        }
    }

    /**
     * Resets the invalidation state to valid.
     *
     * This method should be called after the store has been repopulated with fresh data.
     */
    suspend fun resetInvalidationState() = mutex.withLock {
        _invalidationState.value = InvalidationState.VALID
    }
}

