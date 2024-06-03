package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Quantifiable

data class FetchingState<Id : Comparable<Id>, Q: Quantifiable<Id>, K : Any>(
    val minItemAccessedSoFar: Q? = null,
    val maxItemAccessedSoFar: Q? = null,

    val minRequestSoFar: K? = null,
    val maxRequestSoFar: K? = null,

    val minItemLoadedSoFar: Q? = null,
    val maxItemLoadedSoFar: Q? = null, // Last loaded item

    val currentBackwardPrefetchOffset: Q? = null, // Id of the current offset for backward prefetching
    val currentForwardPrefetchOffset: Q? = null, // Id of the current offset for forward prefetching

)