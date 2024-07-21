package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.Operation
import org.mobilenativefoundation.storex.paging.runtime.OperationManager

/**
 * This file contains a thread-safe implementation of OperationManager for Kotlin Multiplatform.
 *
 * Key design decisions:
 * 1. Use of atomicfu for lock-free reads and atomic updates.
 * 2. Mutex for synchronizing write operations.
 * 3. Immutable list snapshots for consistent reads without locking.
 * 4. Suspension on write operations to work well with coroutines.
 *
 * Trade-offs:
 * - Reads are very fast and non-blocking, but they may not always reflect the absolute latest state.
 * - Writes are serialized and may be slower, but they ensure consistency.
 *
 * Note: This implementation requires the kotlinx-atomicfu dependency.
 */

/**
 * A thread-safe implementation of OperationManager that allows for non-blocking reads
 * and synchronized writes.
 *
 * @param ItemId The type of the item identifier.
 * @param PageRequestKey The type of the paging key.
 * @param ItemValue The type of the item value.
 */
class ConcurrentOperationManager<ItemId: Any, PageRequestKey: Any, ItemValue: Any> :
    OperationManager<ItemId, PageRequestKey, ItemValue> {

    // Atomic reference to the list of operations, allowing for lock-free reads
    private val operationsAtomic = atomic(listOf<Operation<ItemId, PageRequestKey, ItemValue>>())

    // Mutex for synchronizing write operations
    private val writeMutex = Mutex()

    /**
     * Adds an operation to the manager if it's not already present.
     *
     * @param operation The operation to add.
     */
    override suspend fun add(operation: Operation<ItemId, PageRequestKey, ItemValue>) {
        writeMutex.withLock {
            // Atomic update ensures that the change is visible to all threads immediately
            operationsAtomic.updateAndGet { current ->
                if (operation !in current) current + operation else current
            }
        }
    }

    /**
     * Removes a specific operation from the manager.
     *
     * @param operation The operation to remove.
     */
    override suspend fun remove(operation: Operation<ItemId, PageRequestKey, ItemValue>) {
        writeMutex.withLock {
            operationsAtomic.updateAndGet { it - operation }
        }
    }

    /**
     * Removes all operations that match the given predicate.
     *
     * @param predicate A function that determines which operations to remove.
     */
    override suspend fun removeAll(predicate: (Operation<ItemId, PageRequestKey, ItemValue>) -> Boolean) {
        writeMutex.withLock {
            operationsAtomic.updateAndGet { it.filterNot(predicate) }
        }
    }

    /**
     * Clears all operations from the manager.
     */
    override suspend fun clear() {
        writeMutex.withLock {
            operationsAtomic.value = emptyList()
        }
    }

    /**
     * Retrieves a snapshot of all current operations.
     *
     * This is a non-blocking operation and returns an immutable list.
     *
     * @return A list of all current operations.
     */
    override fun get(): List<Operation<ItemId, PageRequestKey, ItemValue>> {
        // Lock-free read of the current state
        return operationsAtomic.value
    }

    /**
     * Retrieves a filtered snapshot of operations that match the given predicate.
     *
     * This is a non-blocking operation and returns an immutable list.
     *
     * @param predicate A function that determines which operations to include.
     * @return A filtered list of operations.
     */
    override fun get(predicate: (Operation<ItemId, PageRequestKey, ItemValue>) -> Boolean): List<Operation<ItemId, PageRequestKey, ItemValue>> {
        // Lock-free read and filter
        return operationsAtomic.value.filter(predicate)
    }
}