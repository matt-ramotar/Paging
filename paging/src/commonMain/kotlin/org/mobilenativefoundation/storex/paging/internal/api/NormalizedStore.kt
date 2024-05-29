package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.internal.impl.PageLoadStatus
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.Quantifiable
import org.mobilenativefoundation.storex.paging.SelfUpdatingItem
import org.mobilenativefoundation.storex.paging.SelfUpdatingPage

interface NormalizedStore<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> {
    fun selfUpdatingItem(id: Quantifiable<Id>): SelfUpdatingItem<Id, V, E>
    fun selfUpdatingPage(key: K): SelfUpdatingPage<Id, K, V, E>
    fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadStatus<Id, K, V, E>>
    fun clear(key: K)
    fun invalidate()
}
