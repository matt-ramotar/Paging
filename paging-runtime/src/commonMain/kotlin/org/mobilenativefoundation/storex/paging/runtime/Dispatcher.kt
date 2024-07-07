package org.mobilenativefoundation.storex.paging.runtime

interface Dispatcher<K : Comparable<K>> {
    suspend fun dispatch(action: Action<K>)
}