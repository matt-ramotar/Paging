package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.Quantifiable

class RealPagingSource<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    private val fetcher: Fetcher<PagingSource.LoadParams<K>, PagingSource.LoadResult.Data<Id, Q, K, V, E>>
) : PagingSource<Id, Q, K, V, E> {
    override suspend fun load(params: PagingSource.LoadParams<K>): PagingSource.LoadResult<Id, Q, K, V, E> {
        TODO("Not yet implemented")
    }

}