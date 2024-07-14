package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.PagingSource

interface RetryBookkeeper<Id : Identifier<Id>, K : Comparable<K>> {
    suspend fun getCount(params: PagingSource.LoadParams<K>): Int
    suspend fun incrementCount(params: PagingSource.LoadParams<K>)
    suspend fun resetCount(params: PagingSource.LoadParams<K>)
}