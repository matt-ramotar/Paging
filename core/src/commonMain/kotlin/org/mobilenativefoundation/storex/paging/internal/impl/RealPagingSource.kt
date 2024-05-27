package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.PagingSource

class RealPagingSource<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
    private val fetcher: Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, K, V, E>>
):
        PagingSource<Id, K, V, E> {
    override suspend fun load(params: PagingSource.LoadParams<K>): PagingSource.LoadResult<Id, K, V, E> {
        TODO("Not yet implemented")
    }

}