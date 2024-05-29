package org.mobilenativefoundation.storex.paging

data class PageState<Id : Comparable<Id>, K: Any, V : Identifiable<Id>, E : Any>(
    val items: List<V>,
    val loadState: SingleLoadState<E>
)