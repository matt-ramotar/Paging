package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.LoadPageStatus
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.SelfUpdatingItem
import org.mobilenativefoundation.storex.paging.SelfUpdatingPage

interface NormalizedStore<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> {
    fun selfUpdatingItem(id: Id): SelfUpdatingItem<Id, V, E>
    fun selfUpdatingPage(key: K): SelfUpdatingPage<Id, K, V, E>
    fun loadPage(params: PagingSource.LoadParams<K>): Flow<LoadPageStatus<Id, K, V, E>>
    fun clear(key: K)
    fun invalidate()
}
