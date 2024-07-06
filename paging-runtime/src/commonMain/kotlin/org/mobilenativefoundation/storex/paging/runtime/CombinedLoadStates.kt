package org.mobilenativefoundation.storex.paging.runtime

data class CombinedLoadStates(
    val refresh: LoadState,
    val prepend: LoadState,
    val append: LoadState,
    val pagingSource: LoadStates,
    val remoteMediator: LoadStates? = null
) {
    companion object {

        fun initial() = CombinedLoadStates(
            refresh = LoadState.NotLoading(false),
            append = LoadState.NotLoading(false),
            prepend = LoadState.NotLoading(false),
            pagingSource = LoadStates(
                refresh = LoadState.NotLoading(false),
                prepend = LoadState.NotLoading(false),
                append = LoadState.NotLoading(false),
            )
        )

        fun fetchingEnd() = CombinedLoadStates(
            refresh = LoadState.NotLoading(false),
            append = LoadState.Loading,
            prepend = LoadState.NotLoading(false),
            pagingSource = LoadStates(
                refresh = LoadState.NotLoading(false),
                prepend = LoadState.NotLoading(false),
                append = LoadState.Loading,
            )
        )

        fun fetchingStart() = CombinedLoadStates(
            refresh = LoadState.NotLoading(false),
            append = LoadState.NotLoading(false),
            prepend = LoadState.Loading,
            pagingSource = LoadStates(
                refresh = LoadState.NotLoading(false),
                prepend = LoadState.Loading,
                append = LoadState.NotLoading(false),
            )
        )
    }
}



