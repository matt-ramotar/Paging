package org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.impl

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItemProvider
import org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.api.UpdatingItemFactory

internal class RealUpdatingItemProvider<Id : Identifier<Id>, V : Identifiable<Id>>(
    private val factory: UpdatingItemFactory<Id, V>
) : UpdatingItemProvider<Id, V> {

    private val itemsAtomic = atomic(mapOf<Id, UpdatingItem<Id, V>>())
    private val writeMutex = Mutex()

    override suspend fun get(id: Id): UpdatingItem<Id, V> {
        // Attempt a lock-free read first
        itemsAtomic.value[id]?.let { return it }

        // If item doesn't exist, we need to create it under a lock
        return createAndAddItem(id)
    }

    private suspend fun createAndAddItem(id: Id): UpdatingItem<Id, V> {
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

    override suspend fun remove(id: Id) {
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