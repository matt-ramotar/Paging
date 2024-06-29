package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.ItemSnapshotList

/**
 * A strategy for validating the paged data against specific rules or constraints.
 */
fun interface ValidationStrategy<Id : Identifier<*>, V : Identifiable<Id>> :
    TransformationStrategy<Id, V> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, V>,
    ): ItemSnapshotList<Id, V>
}
