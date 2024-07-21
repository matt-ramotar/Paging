package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.runtime.Operation
import org.mobilenativefoundation.storex.paging.runtime.OperationManager
import org.mobilenativefoundation.storex.paging.runtime.PagingState
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.MutableOperationPipeline
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.OperationApplier

/**
 * A thread-safe implementation of OperationApplier that applies operations to item snapshots.
 *
 * This class manages a set of operations that can be applied to item snapshots. It ensures
 * thread-safety through the use of a Mutex for all operations that modify shared state.
 *
 * @param ItemId The type of the item identifier.
 * @param PageRequestKey The type of the paging key.
 * @param ItemValue The type of the item value.
 */
class ConcurrentOperationApplier<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    private val operationManager: OperationManager<ItemId, PageRequestKey, ItemValue>,
    private val mutableOperationPipeline: MutableOperationPipeline<ItemId, PageRequestKey, ItemValue>
) : OperationApplier<ItemId, PageRequestKey, ItemValue> {

    // Mutex for ensuring thread-safe access to shared resources
    private val mutex = Mutex()

    // Cache for operation results to improve performance
    private val operationCache = mutableMapOf<CacheKey<ItemId, PageRequestKey, ItemValue>, ItemSnapshotList<ItemId, ItemValue>>()

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
        snapshot: ItemSnapshotList<ItemId, ItemValue>,
        key: PageRequestKey?,
        pagingState: PagingState<ItemId, PageRequestKey, ItemValue>,
        fetchingState: FetchingState<ItemId, PageRequestKey>
    ): ItemSnapshotList<ItemId, ItemValue> = mutex.withLock {
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
    private data class CacheKey<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
        val operation: Operation<ItemId, PageRequestKey, ItemValue>,
        val snapshot: ItemSnapshotList<ItemId, ItemValue>,
        val key: PageRequestKey?,
        val pagingState: PagingState<ItemId, PageRequestKey, ItemValue>,
        val fetchingState: FetchingState<ItemId, PageRequestKey>
    )
}

