package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.ItemSnapshotList

/**
 * A strategy for removing duplicate items from the paged data based on a specific criteria.
 */
fun interface DeduplicationStrategy<Id : Identifier<*>, V : Identifiable<Id>> :
    TransformationStrategy<Id, V> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, V>
    ): ItemSnapshotList<Id, V>
}
