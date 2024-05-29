package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList

fun interface SideEffect<Id : Comparable<Id>, V : Identifiable<Id>> {
    operator fun invoke(snapshot: ItemSnapshotList<Id, V>)
}