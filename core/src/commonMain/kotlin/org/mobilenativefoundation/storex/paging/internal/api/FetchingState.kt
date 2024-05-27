package org.mobilenativefoundation.storex.paging.internal.api

data class FetchingState<Id : Comparable<Id>>(
    val minItemAccessedSoFar: Id? = null,
    val maxItemAccessedSoFar: Id? = null,

    val minItemLoadedSoFar: Id? = null,
    val maxItemLoadedSoFar: Id? = null, // Last loaded item

    val currentBackwardPrefetchOffset: Id? = null, // Id of the current offset for backward prefetching
    val currentForwardPrefetchOffset: Id? = null, // Id of the current offset for forward prefetching
)