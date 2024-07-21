package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.PagingSource

interface RetryBookkeeper<ItemId: Any, PageRequestKey: Any> {
    suspend fun getCount(params: PagingSource.LoadParams<PageRequestKey>): Int
    suspend fun incrementCount(params: PagingSource.LoadParams<PageRequestKey>)
    suspend fun resetCount(params: PagingSource.LoadParams<PageRequestKey>)
}