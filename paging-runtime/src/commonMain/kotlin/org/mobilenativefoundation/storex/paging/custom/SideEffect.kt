package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.runtime.ItemSnapshotList

fun interface SideEffect<ItemId: Any, ItemValue: Any> {
    operator fun invoke(snapshot: ItemSnapshotList<ItemId, ItemValue>)
}