package org.mobilenativefoundation.storex.paging.runtime

data class LoadStates(
    val refresh: LoadState,
    val prepend: LoadState,
    val append: LoadState,
) {
    val hasError =
        refresh is LoadState.Error || append is LoadState.Error || prepend is LoadState.Error
    val isIdle =
        refresh is LoadState.NotLoading && append is LoadState.NotLoading && prepend is LoadState.NotLoading
}
