package org.mobilenativefoundation.storex.paging.runtime.internal.store.api

import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem

/**
 * Represents a store for managing individual items.
 *
 * @param Id The type of the item identifier.
 * @param V The type of the item value, which must be Identifiable by Id.
 */
internal interface ItemStore<Id : Identifier<Id>, V : Identifiable<Id>> {
    /**
     * Retrieves an item by its identifier.
     *
     * @param id The identifier of the item.
     * @return The item if found, null otherwise.
     */
    suspend fun getItem(id: Id): V?

    /**
     * Retrieves a self-updating item for the given identifier.
     *
     * @param id The identifier of the item.
     * @return A UpdatingItem for the given identifier.
     */
    fun getUpdatingItem(id: Id): UpdatingItem<Id, V>
}