package org.mobilenativefoundation.store5.cache

import org.mobilenativefoundation.store.cache5.Cache
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.PagingSource

interface NormalizedCache<Id : Comparable<Id>, K : Any, V : Identifiable<Id>> : Cache<Id, V> {

    fun normalize(key: PagingSource.LoadParams<K>, values: List<V>)
    fun getAllPresent(key: PagingSource.LoadParams<K>): List<Id>
}
