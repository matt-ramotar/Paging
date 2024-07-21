package org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.api

import org.mobilenativefoundation.storex.paging.runtime.PagingSource

// TODO(): Use store/cache
typealias PageMemoryCache<ItemId, PageRequestKey, ItemValue> = MutableMap<PageRequestKey, PagingSource.LoadResult.Data<ItemId, PageRequestKey, ItemValue>>
typealias ItemMemoryCache<ItemId, ItemValue> = MutableMap<ItemId, ItemValue>