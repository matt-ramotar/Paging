package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.internal.impl.PageLoadState

interface NormalizedStore<Id : Identifier<*>, K : Any, V : Identifiable<Id>> {
    fun selfUpdatingItem(id: Id): SelfUpdatingItem<Id, V>
    fun selfUpdatingPage(key: K): SelfUpdatingPage<Id, V>
    fun loadPage(params: PagingSource.LoadParams<K>): Flow<PageLoadState<Id, K, V>>
    fun getItem(id: Id): V?
    fun clear(key: K)
    fun invalidate()
}


