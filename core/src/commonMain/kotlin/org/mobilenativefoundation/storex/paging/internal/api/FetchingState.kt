package org.mobilenativefoundation.storex.paging.internal.api

data class FetchingState<Id : Comparable<Id>>(
    val maxItemAccessedSoFar: Id? = null,
    val maxItemLoadedSoFar: Id? = null, // Last loaded item
    val currentPrefetchOffset: Id? = null, // Id of the current offset for prefetching
)