package org.mobilenativefoundation.storex.paging

sealed class PagingLoadState(
    val endOfPaginationReached: Boolean,
) {
    class NotLoading(
        endOfPaginationReached: Boolean,
    ) : PagingLoadState(endOfPaginationReached) {
        internal companion object {
            val Complete = NotLoading(endOfPaginationReached = true)
            val Incomplete = NotLoading(endOfPaginationReached = false)
        }
    }

    data object Loading : PagingLoadState(endOfPaginationReached = false)

    data class Error(
        val error: Throwable
    ) : PagingLoadState(endOfPaginationReached = false)
}
