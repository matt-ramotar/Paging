package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.internal.api.PagingBuffer

internal fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> PagingBuffer<Id, K, V, E>.snapshotFrom(
    ids: List<Id>,
    onSnapshot: (snapshot: ItemSnapshotList<Id, V>) -> Unit = {}
): ItemSnapshotList<Id, V> {
    val items = getAllItems(ids).map { it.value }
    return ItemSnapshotList(items).also(onSnapshot)
}