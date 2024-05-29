package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Quantifiable

data class FetchingState<Id : Comparable<Id>>(
    val minItemAccessedSoFar: Quantifiable<Id>? = null,
    val maxItemAccessedSoFar: Quantifiable<Id>? = null,

    val minItemLoadedSoFar: Quantifiable<Id>? = null,
    val maxItemLoadedSoFar: Quantifiable<Id>? = null, // Last loaded item

    val currentBackwardPrefetchOffset: Quantifiable<Id>? = null, // Id of the current offset for backward prefetching
    val currentForwardPrefetchOffset: Quantifiable<Id>? = null, // Id of the current offset for forward prefetching
)