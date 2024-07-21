package org.mobilenativefoundation.storex.paging.runtime

/**
 * Represents an operation that can be applied to an item snapshot.
 *
 * @param ItemId The type of the item identifier.
 * @param PageRequestKey The type of the paging key.
 * @param ItemValue The type of the item value.
 */
abstract class Operation<ItemId: Any, PageRequestKey: Any, ItemValue: Any> {
    /**
     * Determines whether this operation should be applied based on the current state.
     *
     * @param key The current paging key.
     * @param pagingState The current paging state.
     * @param fetchingState The current fetching state.
     * @return True if the operation should be applied, false otherwise.
     */
    internal abstract fun shouldApply(key: PageRequestKey?, pagingState: PagingState<ItemId, PageRequestKey, ItemValue>, fetchingState: FetchingState<ItemId, PageRequestKey>): Boolean

    /**
     * Applies the operation to the given snapshot.
     *
     * @param snapshot The input snapshot.
     * @param key The current paging key.
     * @param pagingState The current paging state.
     * @param fetchingState The current fetching state.
     * @return The transformed snapshot after applying the operation.
     */
    internal abstract fun apply(
        snapshot: ItemSnapshotList<ItemId, ItemValue>,
        key: PageRequestKey?,
        pagingState: PagingState<ItemId, PageRequestKey, ItemValue>,
        fetchingState: FetchingState<ItemId, PageRequestKey>
    ): ItemSnapshotList<ItemId, ItemValue>
}