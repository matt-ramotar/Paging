package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.ItemSnapshotList

/**
 * A strategy for grouping the paged data based on a specific criteria or field.
 */
fun interface GroupingStrategy<Id : Identifier<*>, V : Identifiable<Id>> :
    TransformationStrategy<Id, V> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, V>,
    ): ItemSnapshotList<Id, V>
}
