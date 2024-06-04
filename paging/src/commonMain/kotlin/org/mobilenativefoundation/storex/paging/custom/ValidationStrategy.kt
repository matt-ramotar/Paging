package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.Quantifiable

/**
 * A strategy for validating the paged data against specific rules or constraints.
 */
fun interface ValidationStrategy<Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>> :
    TransformationStrategy<Id, Q, V> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, Q, V>,
    ): ItemSnapshotList<Id, Q, V>
}
