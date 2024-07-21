package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.runtime.PagingState

/**
 * Applies operations to the snapshot.
 */
internal interface OperationApplier<ItemId: Any, PageRequestKey: Any, ItemValue: Any> {
    suspend fun applyOperations(
        snapshot: ItemSnapshotList<ItemId, ItemValue>,
        key: PageRequestKey?,
        pagingState: PagingState<ItemId, PageRequestKey, ItemValue>,
        fetchingState: FetchingState<ItemId, PageRequestKey>
    ): ItemSnapshotList<ItemId, ItemValue>
}