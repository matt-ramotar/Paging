package org.mobilenativefoundation.storex.paging.runtime

interface PlaceholderFactory<Id : Identifier<Id>,K: Comparable<K>, V : Identifiable<Id>> {
    fun create(index: Int, params: PagingSource.LoadParams<K>): V
}