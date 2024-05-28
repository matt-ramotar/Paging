package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList

/**
 * A strategy for sorting the paged data based on specific sorting parameters.
 */
fun interface SortingStrategy<Id : Comparable<Id>, V : Identifiable<Id>, P : Any, T : P> :
    TransformationStrategy<Id, V, T> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, V>,
        params: T
    ): ItemSnapshotList<Id, V>
}
