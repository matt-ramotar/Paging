package org.mobilenativefoundation.storex.paging.runtime

sealed class LoadState(
    val endOfPaginationReached: Boolean,
) {
    class NotLoading(
        endOfPaginationReached: Boolean,
    ) : LoadState(endOfPaginationReached) {
        internal companion object {
            val Complete = NotLoading(endOfPaginationReached = true)
            val Incomplete = NotLoading(endOfPaginationReached = false)
        }
    }

    data object Loading : LoadState(endOfPaginationReached = false)

    data class Error(
        val error: Throwable
    ) : LoadState(endOfPaginationReached = false)
}
