package org.mobilenativefoundation.storex.paging.runtime

/**
 * Represents an operation that can be applied to an item snapshot.
 *
 * @param Id The type of the item identifier.
 * @param K The type of the paging key.
 * @param V The type of the item value.
 */
interface Operation<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    /**
     * Determines whether this operation should be applied based on the current state.
     *
     * @param key The current paging key.
     * @param pagingState The current paging state.
     * @param fetchingState The current fetching state.
     * @return True if the operation should be applied, false otherwise.
     */
    fun shouldApply(key: K?, pagingState: PagingState<Id>, fetchingState: FetchingState<Id, K>): Boolean

    /**
     * Applies the operation to the given snapshot.
     *
     * @param snapshot The input snapshot.
     * @param key The current paging key.
     * @param pagingState The current paging state.
     * @param fetchingState The current fetching state.
     * @return The transformed snapshot after applying the operation.
     */
    fun apply(
        snapshot: ItemSnapshotList<Id, V>,
        key: K?,
        pagingState: PagingState<Id>,
        fetchingState: FetchingState<Id, K>
    ): ItemSnapshotList<Id, V>
}