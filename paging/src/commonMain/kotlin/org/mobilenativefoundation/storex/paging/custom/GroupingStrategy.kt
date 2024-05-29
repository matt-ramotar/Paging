package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList

/**
 * A strategy for grouping the paged data based on a specific criteria or field.
 */
fun interface GroupingStrategy<Id : Comparable<Id>, V : Identifiable<Id>, P : Any, T : P> :
    TransformationStrategy<Id, V, T> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, V>,
        params: T
    ): ItemSnapshotList<Id, V>
}
