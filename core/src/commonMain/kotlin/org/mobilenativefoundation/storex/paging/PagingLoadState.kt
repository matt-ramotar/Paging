package org.mobilenativefoundation.storex.paging

import kotlinx.serialization.json.JsonObject

sealed class PagingLoadState<out E : Any>(
    val endOfPaginationReached: Boolean,
    val extras: JsonObject? = null
) {
    class NotLoading(
        endOfPaginationReached: Boolean,
        extras: JsonObject? = null
    ) : PagingLoadState<Nothing>(endOfPaginationReached, extras) {
        internal companion object {
            val Complete = NotLoading(endOfPaginationReached = true)
            val Incomplete = NotLoading(endOfPaginationReached = false)
        }
    }

    class Loading(extras: JsonObject? = null) :
        PagingLoadState<Nothing>(endOfPaginationReached = false, extras)

    class Error<E : Any>(
        val error: E,
        extras: JsonObject? = null,
    ) : PagingLoadState<E>(endOfPaginationReached = false, extras)

}
