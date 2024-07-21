package org.mobilenativefoundation.storex.paging.runtime


data class PagingState<ItemId : Any>(
    val ids: List<ItemId?>,
    val loadStates: CombinedLoadStates,
) {
    companion object {
        fun <ItemId: Any> initial() = PagingState<ItemId>(
            emptyList(),
            CombinedLoadStates.initial()
        )
    }
}
