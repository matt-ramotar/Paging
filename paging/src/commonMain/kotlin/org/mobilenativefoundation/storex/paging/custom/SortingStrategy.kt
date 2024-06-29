package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.Identifier

/**
 * A strategy for sorting the paged data based on specific sorting parameters.
 */
fun interface SortingStrategy<Id : Identifier<*>, V : Identifiable<Id>> :
    TransformationStrategy<Id,  V> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id,  V>,
    ): ItemSnapshotList<Id,  V>
}
