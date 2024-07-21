package org.mobilenativefoundation.storex.paging.runtime


data class PagingState<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    val ids: List<ItemId?>,
    val loadStates: CombinedLoadStates,
) {
    companion object {
        fun <ItemId : Any, PageRequestKey : Any, ItemValue : Any> initial() = PagingState<ItemId, PageRequestKey, ItemValue>(
            emptyList(),
            CombinedLoadStates.initial(),
        )
    }
}
