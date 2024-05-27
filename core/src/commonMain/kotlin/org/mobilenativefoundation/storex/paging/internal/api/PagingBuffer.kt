package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.PagingConfig
import org.mobilenativefoundation.storex.paging.PagingSource

/**
 * A custom data structure for efficiently storing and retrieving paging data.
 * The [PagingBuffer] is responsible for caching and providing access to the loaded pages of data.
 * It allows retrieving data by load parameters, page key, or accessing the entire buffer.
 */
interface PagingBuffer<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> {
    fun get(params: PagingSource.LoadParams<K>): NormalizedPagingData.Page<Id, K, V, E>?

    fun get(key: K): NormalizedPagingData.Page<Id, K, V, E>?

    fun get(id: Id): NormalizedPagingData.Item<Id, K, V, E>?
    fun getPageContaining(id: Id): NormalizedPagingData.Page<Id, K, V, E>?
    fun getNextPage(page: NormalizedPagingData.Page<Id, K, V, E>): NormalizedPagingData.Page<Id, K, V, E>?

    fun head(): NormalizedPagingData.Page<Id, K, V, E>?

    fun getAll(): List<NormalizedPagingData.Page<Id, K, V, E>>

    fun getAllItems(): List<NormalizedPagingData.Item<Id, K, V, E>>
    fun getAllItems(ids: List<Id>): List<NormalizedPagingData.Item<Id, K, V, E>>

    fun isEmpty(): Boolean

    fun indexOf(key: K): Int
    fun positionOf(id: Id): Int
    fun minDistanceBetween(a: Id, b: Id): Int

    fun size(): Int

    fun getItemsInRange(
        anchorPosition: K,
        prefetchPosition: K?,
        pagingConfig: PagingConfig
    ): List<NormalizedPagingData.Item<Id, K, V, E>>
}