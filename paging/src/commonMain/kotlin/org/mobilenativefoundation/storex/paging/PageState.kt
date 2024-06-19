package org.mobilenativefoundation.storex.paging

data class PageState<Id : Identifier<*>, V : Identifiable<Id>>(
    val items: List<V>,
    val loadState: SingleLoadState
)