package org.mobilenativefoundation.storex.paging.runtime.internal.store.api

import org.mobilenativefoundation.storex.paging.persistence.api.PersistenceResult

/**
 * Represents the capability to invalidate all data in a store.
 */
interface StoreInvalidation {
    /**
     * Invalidates all data in the store.
     */
    suspend fun invalidateAll(): PersistenceResult<Unit>
}