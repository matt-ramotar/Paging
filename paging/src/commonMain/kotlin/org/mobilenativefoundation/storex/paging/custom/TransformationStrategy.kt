package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.Quantifiable

interface TransformationStrategy<Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>> {

    operator fun invoke(
        snapshot: ItemSnapshotList<Id, Q, V>,
    ): ItemSnapshotList<Id, Q, V>
}