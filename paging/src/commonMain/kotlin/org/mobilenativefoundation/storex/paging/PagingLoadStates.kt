package org.mobilenativefoundation.storex.paging

data class PagingLoadStates<E: Any>(
    val refresh: PagingLoadState<E>,
    val prepend: PagingLoadState<E>,
    val append: PagingLoadState<E>,
) {
    val hasError =
        refresh is PagingLoadState.Error || append is PagingLoadState.Error || prepend is PagingLoadState.Error
    val isIdle =
        refresh is PagingLoadState.NotLoading && append is PagingLoadState.NotLoading && prepend is PagingLoadState.NotLoading
}
