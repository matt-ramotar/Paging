package org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.impl

import kotlinx.coroutines.flow.MutableSharedFlow
import org.mobilenativefoundation.storex.paging.runtime.Action
import org.mobilenativefoundation.storex.paging.runtime.Dispatcher

class RealDispatcher<K : Comparable<K>>(
    private val actionsFlow: MutableSharedFlow<Action<K>>
) : Dispatcher<K> {
    override suspend fun dispatch(action: Action<K>) {
        actionsFlow.emit(action)
    }
}