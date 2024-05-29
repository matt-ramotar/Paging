package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.PagingSource

/**
 * Represents a mutable version of [PagingBuffer] that allows adding and updating paging data.
 */
interface MutablePagingBuffer<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> :
    PagingBuffer<Id, K, V, E> {

    /**
     * Puts the loaded page of data associated with the specified [PagingSource.LoadParams] into the buffer.
     */
    fun append(params: PagingSource.LoadParams<K>, page: PagingSource.LoadResult.Data<Id, K, V, E>)

    fun prepend(params: PagingSource.LoadParams<K>, page: PagingSource.LoadResult.Data<Id, K, V, E>)
    fun remove(params: PagingSource.LoadParams<K>)
}