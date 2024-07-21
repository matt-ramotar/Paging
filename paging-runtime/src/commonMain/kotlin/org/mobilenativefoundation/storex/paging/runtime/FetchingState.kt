package org.mobilenativefoundation.storex.paging.runtime

data class FetchingState<ItemId : Any, PageRequestKey : Any>(
    val minItemAccessedSoFar: ItemId? = null,
    val maxItemAccessedSoFar: ItemId? = null,

    val minRequestSoFar: PageRequestKey? = null,
    val maxRequestSoFar: PageRequestKey? = null,

    val minItemLoadedSoFar: ItemId? = null,
    val maxItemLoadedSoFar: ItemId? = null,

    val currentBackwardPrefetchOffset: ItemId? = null,
    val currentForwardPrefetchOffset: ItemId? = null,
)