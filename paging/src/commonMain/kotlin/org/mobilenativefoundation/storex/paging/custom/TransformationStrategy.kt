package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList

interface TransformationStrategy<Id : Comparable<Id>, V : Identifiable<Id>, P: Any> {

    operator fun invoke(
        snapshot: ItemSnapshotList<Id, V>,
        params: P
    ): ItemSnapshotList<Id, V>
}