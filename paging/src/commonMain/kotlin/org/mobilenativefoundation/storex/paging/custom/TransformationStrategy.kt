package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.ItemSnapshotList

fun interface TransformationStrategy<Id : Identifier<*>, V : Identifiable<Id>> {

    operator fun invoke(
        snapshot: ItemSnapshotList<Id, V>,
    ): ItemSnapshotList<Id, V>
}