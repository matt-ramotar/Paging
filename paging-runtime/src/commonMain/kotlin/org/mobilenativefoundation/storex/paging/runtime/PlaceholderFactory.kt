package org.mobilenativefoundation.storex.paging.runtime

interface PlaceholderFactory<ItemId : Any, PageRequestKey : Any, ItemValue : Any> {
    fun create(index: Int, params: PagingSource.LoadParams<PageRequestKey>): ItemValue
}