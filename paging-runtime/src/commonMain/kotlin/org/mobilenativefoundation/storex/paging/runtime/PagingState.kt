package org.mobilenativefoundation.storex.paging.runtime


data class PagingState<Id : Identifier<*>>(
    val ids: List<Id?>,
    val loadStates: CombinedLoadStates,
) {
    companion object {
        fun <Id : Identifier<*>> initial() = PagingState<Id>(
            emptyList(),
            CombinedLoadStates.initial()
        )
    }
}
