package org.mobilenativefoundation.storex.paging.runtime

interface Dispatcher<K : Comparable<K>> {
    fun dispatch(request: Action<K>)
}