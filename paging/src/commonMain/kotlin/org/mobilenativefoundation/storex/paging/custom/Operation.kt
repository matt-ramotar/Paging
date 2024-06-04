package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.PagingState
import org.mobilenativefoundation.storex.paging.Quantifiable
import org.mobilenativefoundation.storex.paging.internal.api.FetchingState

abstract class Operation<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>> {

    abstract operator fun invoke(snapshot: ItemSnapshotList<Id, Q, V>): ItemSnapshotList<Id, Q, V>

    open fun shouldApply(key: K?, pagingState: PagingState<Id, Q, *>, fetchingState: FetchingState<Id, Q, K>): Boolean =
        true
}
