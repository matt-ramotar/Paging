package org.mobilenativefoundation.storex.paging

data class CombinedPagingLoadStates<E : Any>(
    val refresh: PagingLoadState<E>,
    val prepend: PagingLoadState<E>,
    val append: PagingLoadState<E>,
    val pagingSource: PagingLoadStates<E>,
    val remoteMediator: PagingLoadStates<E>? = null
) {
    companion object {

        fun <E : Any> initial() = CombinedPagingLoadStates<E>(
            refresh = PagingLoadState.NotLoading(false),
            append = PagingLoadState.NotLoading(false),
            prepend = PagingLoadState.NotLoading(false),
            pagingSource = PagingLoadStates(
                refresh = PagingLoadState.NotLoading(false),
                prepend = PagingLoadState.NotLoading(false),
                append = PagingLoadState.NotLoading(false),
            )
        )

        fun <E : Any> fetchingEnd() = CombinedPagingLoadStates<E>(
            refresh = PagingLoadState.NotLoading(false),
            append = PagingLoadState.Loading(),
            prepend = PagingLoadState.NotLoading(false),
            pagingSource = PagingLoadStates(
                refresh = PagingLoadState.NotLoading(false),
                prepend = PagingLoadState.NotLoading(false),
                append = PagingLoadState.Loading(),
            )
        )

        fun <E : Any> fetchingStart() = CombinedPagingLoadStates<E>(
            refresh = PagingLoadState.NotLoading(false),
            append = PagingLoadState.NotLoading(false),
            prepend = PagingLoadState.Loading(),
            pagingSource = PagingLoadStates(
                refresh = PagingLoadState.NotLoading(false),
                prepend = PagingLoadState.Loading(),
                append = PagingLoadState.NotLoading(false),
            )
        )
    }
}



