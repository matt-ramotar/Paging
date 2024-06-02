package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.Quantifiable

/**
 * A strategy for removing duplicate items from the paged data based on a specific criteria.
 */
fun interface DeduplicationStrategy<Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, P : Any, T : P> :
    TransformationStrategy<Id, Q, V, T> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, Q, V>,
        params: T
    ): ItemSnapshotList<Id, Q, V>
}
