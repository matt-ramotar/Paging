package org.mobilenativefoundation.storex.paging

data class PageState<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    val items: List<V>,
    val loadState: SingleLoadState<E>
)