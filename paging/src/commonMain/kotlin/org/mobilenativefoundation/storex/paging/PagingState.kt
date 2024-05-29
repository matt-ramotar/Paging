package org.mobilenativefoundation.storex.paging

class PagingState<Id : Comparable<Id>, E : Any>(
    val ids: List<Quantifiable<Id>?>,
    val loadStates: CombinedPagingLoadStates<E>
) {
    companion object {
        fun <Id : Comparable<Id>, E : Any> initial() = PagingState<Id, E>(
            emptyList(),
            CombinedPagingLoadStates.initial()
        )
    }
}