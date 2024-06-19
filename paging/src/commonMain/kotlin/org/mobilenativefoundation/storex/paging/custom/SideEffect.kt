package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.Identifier

fun interface SideEffect<Id : Identifier<*>, V : Identifiable<Id>> {
    operator fun invoke(snapshot: ItemSnapshotList<Id, V>)
}