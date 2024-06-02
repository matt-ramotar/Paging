package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.Quantifiable

/**
 * A strategy for sorting the paged data based on specific sorting parameters.
 */
fun interface SortingStrategy<Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>, P : Any, T : P> :
    TransformationStrategy<Id, Q, V, T> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, Q, V>,
        params: T
    ): ItemSnapshotList<Id, Q, V>
}
