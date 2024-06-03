package org.mobilenativefoundation.storex.paging

class PagingState<Id : Comparable<Id>, Q: Quantifiable<Id>, E : Any>(
    val ids: List<Q?>,
    val loadStates: CombinedPagingLoadStates<E>
) {
    companion object {
        fun <Id : Comparable<Id>, Q: Quantifiable<Id>, E : Any> initial() = PagingState<Id, Q, E>(
            emptyList(),
            CombinedPagingLoadStates.initial()
        )
    }
}

