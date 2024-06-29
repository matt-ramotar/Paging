package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Identifier

data class FetchingState<Id : Identifier<*>, K : Any>(
    val minItemAccessedSoFar: Id? = null,
    val maxItemAccessedSoFar: Id? = null,

    val minRequestSoFar: K? = null,
    val maxRequestSoFar: K? = null,

    val minItemLoadedSoFar: Id? = null,
    val maxItemLoadedSoFar: Id? = null,

    val currentBackwardPrefetchOffset: Id? = null,
    val currentForwardPrefetchOffset: Id? = null,
)