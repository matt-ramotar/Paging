package org.mobilenativefoundation.storex.paging.runtime.internal.store.impl

/**
 * Represents the possible states of store invalidation.
 */
internal enum class InvalidationState {
    /** The store data is currently valid. */
    VALID,

    /** The store is in the process of invalidating its data. */
    INVALIDATING,

    /** The store data has been invalidated and needs to be refreshed. */
    INVALID,

    /** An error occurred during the invalidation process. */
    ERROR
}