package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.internal.impl.PageLoadStatus

interface NormalizedStore<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> {
    fun selfUpdatingItem(id: Q): SelfUpdatingItem<Id, Q, V, E>
    fun selfUpdatingPage(key: K): SelfUpdatingPage<Id, Q, K, V, E>
    fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadStatus<Id,Q, K, V, E>>
    fun getItem(id: Q): V?
    fun clear(key: K)
    fun invalidate()
}
