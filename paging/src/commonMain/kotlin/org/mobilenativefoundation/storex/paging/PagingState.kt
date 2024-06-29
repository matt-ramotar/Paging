package org.mobilenativefoundation.storex.paging

class PagingState<Id : Identifier<*>>(
    val ids: List<Id?>,
    val loadStates: CombinedPagingLoadStates,
) {
    companion object {
        fun <Id : Identifier<*>> initial() = PagingState<Id>(
            emptyList(),
            CombinedPagingLoadStates.initial()
        )
    }
}


class PagingStateWithEventSink<Id : Identifier<*>, K : Comparable<K>>(
    val ids: List<Id?>,
    val loadStates: CombinedPagingLoadStates,
    val eventSink: (PagingRequest<K>) -> Unit
)
