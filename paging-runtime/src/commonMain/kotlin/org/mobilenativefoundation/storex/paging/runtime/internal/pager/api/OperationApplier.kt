package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.FetchingState
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.runtime.PagingState

/**
 * Applies operations to the snapshot.
 */
internal interface OperationApplier<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    suspend fun applyOperations(
        snapshot: ItemSnapshotList<Id, V>,
        key: K?,
        pagingState: PagingState<Id>,
        fetchingState: FetchingState<Id, K>
    ): ItemSnapshotList<Id, V>
}