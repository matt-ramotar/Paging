package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.ItemSnapshotList

/**
 * Represents a strategy for aggregating loaded pages of data.
 */

fun interface AggregatingStrategy<Id : Identifier<*>, V : Identifiable<Id>> :
    TransformationStrategy<Id, V> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, V>,
    ): ItemSnapshotList<Id, V>
}
