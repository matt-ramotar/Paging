package org.mobilenativefoundation.storex.paging

import org.mobilenativefoundation.store5.core.Identifiable

data class PageState<Id : Comparable<Id>, K: Any, V : Identifiable<Id>, E : Any>(
    val items: List<V>,
    val loadState: SingleLoadState<E>
)