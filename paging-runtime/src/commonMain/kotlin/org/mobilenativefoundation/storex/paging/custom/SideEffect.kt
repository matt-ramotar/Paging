package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.ItemSnapshotList

fun interface SideEffect<Id : Identifier<*>, V : Identifiable<Id>> {
    operator fun invoke(snapshot: ItemSnapshotList<Id, V>)
}