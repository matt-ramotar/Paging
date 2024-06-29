package org.mobilenativefoundation.storex.paging

data class CombinedPagingLoadStates(
    val refresh: PagingLoadState,
    val prepend: PagingLoadState,
    val append: PagingLoadState,
    val pagingSource: PagingLoadStates,
    val remoteMediator: PagingLoadStates? = null
) {
    companion object {

        fun initial() = CombinedPagingLoadStates(
            refresh = PagingLoadState.NotLoading(false),
            append = PagingLoadState.NotLoading(false),
            prepend = PagingLoadState.NotLoading(false),
            pagingSource = PagingLoadStates(
                refresh = PagingLoadState.NotLoading(false),
                prepend = PagingLoadState.NotLoading(false),
                append = PagingLoadState.NotLoading(false),
            )
        )

        fun fetchingEnd() = CombinedPagingLoadStates(
            refresh = PagingLoadState.NotLoading(false),
            append = PagingLoadState.Loading,
            prepend = PagingLoadState.NotLoading(false),
            pagingSource = PagingLoadStates(
                refresh = PagingLoadState.NotLoading(false),
                prepend = PagingLoadState.NotLoading(false),
                append = PagingLoadState.Loading,
            )
        )

        fun fetchingStart() = CombinedPagingLoadStates(
            refresh = PagingLoadState.NotLoading(false),
            append = PagingLoadState.NotLoading(false),
            prepend = PagingLoadState.Loading,
            pagingSource = PagingLoadStates(
                refresh = PagingLoadState.NotLoading(false),
                prepend = PagingLoadState.Loading,
                append = PagingLoadState.NotLoading(false),
            )
        )
    }
}



