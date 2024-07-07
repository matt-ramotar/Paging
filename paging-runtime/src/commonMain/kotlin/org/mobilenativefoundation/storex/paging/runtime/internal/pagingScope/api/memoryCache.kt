package org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.api

import org.mobilenativefoundation.storex.paging.runtime.PagingSource

// TODO(): Use store/cache
typealias PageMemoryCache<Id,K, V> = MutableMap<K, PagingSource.LoadResult.Data<Id, K, V>>
typealias ItemMemoryCache<Q, V> = MutableMap<Q, V>