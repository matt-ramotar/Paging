package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.runtime.Operation
import org.mobilenativefoundation.storex.paging.runtime.OperationManager
import org.mobilenativefoundation.storex.paging.runtime.PagingState
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.OperationApplier

/**
 * A thread-safe implementation of OperationApplier that applies operations to item snapshots.
 *
 * This class manages a set of operations that can be applied to item snapshots. It ensures
 * thread-safety through the use of a Mutex for all operations that modify shared state.
 *
 * @param Id The type of the item identifier.
 * @param K The type of the paging key.
 * @param V The type of the item value.
 */
class ConcurrentOperationApplier<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val operationManager: OperationManager<Id, K, V>
) : OperationApplier<Id, K, V> {

    // Mutex for ensuring thread-safe access to shared resources
    private val mutex = Mutex()

    // Cache for operation results to improve performance
    private val operationCache = mutableMapOf<CacheKey<Id, K, V>, ItemSnapshotList<Id, V>>()

    /**
     * Applies registered operations to the given snapshot.
     *
     * This method applies all registered operations that should be applied based on the
     * current state and key. It uses caching to improve performance for repeated operations.
     *
     * @param snapshot The original item snapshot.
     * @param key The current paging key.
     * @param pagingState The current paging state.
     * @param fetchingState The current fetching state.
     * @return The transformed item snapshot after applying all applicable operations.
     */
    override suspend fun applyOperations(
        snapshot: ItemSnapshotList<Id, V>,
        key: K?,
        pagingState: PagingState<Id>,
        fetchingState: FetchingState<Id, K>
    ): ItemSnapshotList<Id, V> = mutex.withLock {
        operationManager.get().fold(snapshot) { acc, operation ->
            if (operation.shouldApply(key, pagingState, fetchingState)) {
                val cacheKey = CacheKey(operation, acc, key, pagingState, fetchingState)
                operationCache.getOrPut(cacheKey) {
                    operation.apply(acc, key, pagingState, fetchingState)
                }
            } else {
                acc
            }
        }
    }

    /**
     * A data class representing the cache key for operation results.
     *
     * @param operation The operation being applied.
     * @param snapshot The input snapshot.
     * @param key The paging key.
     * @param pagingState The paging state.
     * @param fetchingState The fetching state.
     */
    private data class CacheKey<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
        val operation: Operation<Id, K, V>,
        val snapshot: ItemSnapshotList<Id, V>,
        val key: K?,
        val pagingState: PagingState<Id>,
        val fetchingState: FetchingState<Id, K>
    )
}

