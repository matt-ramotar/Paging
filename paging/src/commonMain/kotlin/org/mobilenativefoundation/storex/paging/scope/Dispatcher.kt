package org.mobilenativefoundation.storex.paging.scope

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.mobilenativefoundation.storex.paging.PagingRequest

interface Dispatcher<K : Comparable<K>> {
    fun dispatch(request: PagingRequest<K>)
}

class RealDispatcher<K: Comparable<K>>(
    private val requests: MutableSharedFlow<PagingRequest<K>>
): Dispatcher<K> {
    override fun dispatch(request: PagingRequest<K>) {
        TODO("Not yet implemented")
    }

}