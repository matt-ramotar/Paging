package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.Quantifiable

/**
 * A custom data structure for efficiently storing and retrieving paging data.
 * The [PagingBuffer] is responsible for caching and providing access to the loaded pages of data.
 * It allows retrieving data by load parameters, page key, or accessing the entire buffer.
 */
interface PagingBuffer<Id : Comparable<Id>, Q: Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> {
    suspend fun get(params: PagingSource.LoadParams<K>): PagingSource.LoadResult.Normalized<Id, Q, K, V, E>?
    suspend fun get(id: Id): V?
    suspend fun snapshot(ids: List<Id>): ItemSnapshotList<Id, Q, V>
}