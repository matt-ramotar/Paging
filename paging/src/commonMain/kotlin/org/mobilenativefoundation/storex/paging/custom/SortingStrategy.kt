package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.Quantifiable

/**
 * A strategy for sorting the paged data based on specific sorting parameters.
 */
fun interface SortingStrategy<Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>> :
    TransformationStrategy<Id, Q, V> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, Q, V>,
    ): ItemSnapshotList<Id, Q, V>
}
