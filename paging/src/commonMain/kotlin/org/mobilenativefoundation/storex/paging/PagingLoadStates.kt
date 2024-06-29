package org.mobilenativefoundation.storex.paging

data class PagingLoadStates(
    val refresh: PagingLoadState,
    val prepend: PagingLoadState,
    val append: PagingLoadState,
) {
    val hasError =
        refresh is PagingLoadState.Error || append is PagingLoadState.Error || prepend is PagingLoadState.Error
    val isIdle =
        refresh is PagingLoadState.NotLoading && append is PagingLoadState.NotLoading && prepend is PagingLoadState.NotLoading
}
