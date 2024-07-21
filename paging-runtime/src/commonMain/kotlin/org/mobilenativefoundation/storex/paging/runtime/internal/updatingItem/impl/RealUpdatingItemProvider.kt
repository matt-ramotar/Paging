package org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.impl

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItemProvider
import org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.api.UpdatingItemFactory

internal class RealUpdatingItemProvider<ItemId : Any, ItemValue : Any>(
    private val factory: UpdatingItemFactory<ItemId, ItemValue>
) : UpdatingItemProvider<ItemId, ItemValue> {

    private val itemsAtomic = atomic(mapOf<ItemId, UpdatingItem<ItemId, ItemValue>>())
    private val writeMutex = Mutex()

    override suspend fun get(id: ItemId): UpdatingItem<ItemId, ItemValue> {
        // Attempt a lock-free read first
        itemsAtomic.value[id]?.let { return it }

        // If item doesn't exist, we need to create it under a lock
        return createAndAddItem(id)
    }

    private suspend fun createAndAddItem(id: ItemId): UpdatingItem<ItemId, ItemValue> {
        return writeMutex.withLock {
            // Double-check if the item was added by another thread while we were waiting for the lock
            itemsAtomic.value[id]?.let { return@withLock it }

            // Create new item
            val newItem = factory.create(id)

            // Add new item to the map atomically
            itemsAtomic.updateAndGet { current ->
                current + (id to newItem)
            }

            newItem
        }
    }

    override suspend fun remove(id: ItemId) {
        writeMutex.withLock {
            itemsAtomic.updateAndGet { current ->
                current - id
            }
        }
    }

    override suspend fun clear() {
        writeMutex.withLock {
            itemsAtomic.value = emptyMap()
        }
    }
}