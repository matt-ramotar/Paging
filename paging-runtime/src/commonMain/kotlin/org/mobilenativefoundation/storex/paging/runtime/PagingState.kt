package org.mobilenativefoundation.storex.paging.runtime

import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.MutableOperationPipeline


data class PagingState<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    val ids: List<ItemId?>,
    val loadStates: CombinedLoadStates,
    val mutableOperationPipeline: MutableOperationPipeline<ItemId, PageRequestKey, ItemValue>
) {
    companion object {
        fun <ItemId : Any, PageRequestKey : Any, ItemValue : Any> initial() = PagingState<ItemId, PageRequestKey, ItemValue>(
            emptyList(),
            CombinedLoadStates.initial(),
            MutableOperationPipeline.empty()
        )
    }
}
