package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.Quantifiable

/**
 * A strategy for validating the paged data against specific rules or constraints.
 */
fun interface ValidationStrategy<Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>, P : Any, T : P> :
    TransformationStrategy<Id, Q, V, T> {
    override operator fun invoke(
        snapshot: ItemSnapshotList<Id, Q, V>,
        params: T
    ): ItemSnapshotList<Id, Q, V>
}
