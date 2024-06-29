package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState

abstract class Operation<Id : Identifier<*>, K : Any, V : Identifiable<Id>> {

    abstract operator fun invoke(snapshot: ItemSnapshotList<Id, V>): ItemSnapshotList<Id, V>

    open fun shouldApply(key: K?, pagingState: PagingState<Id>, fetchingState: FetchingState<Id, K>): Boolean =
        true
}
