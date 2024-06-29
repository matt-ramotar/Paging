package org.mobilenativefoundation.storex.paging.custom

import org.mobilenativefoundation.storex.paging.PagingSource

fun interface Middleware<K : Any> {
    suspend fun apply(
        params: PagingSource.LoadParams<K>,
        next: suspend (PagingSource.LoadParams<K>) -> PagingSource.LoadParams<K>
    ): PagingSource.LoadParams<K>
}