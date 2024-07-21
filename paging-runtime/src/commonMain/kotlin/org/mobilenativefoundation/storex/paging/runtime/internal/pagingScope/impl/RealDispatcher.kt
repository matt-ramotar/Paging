package org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.impl

import kotlinx.coroutines.flow.MutableSharedFlow
import org.mobilenativefoundation.storex.paging.runtime.Action
import org.mobilenativefoundation.storex.paging.runtime.Dispatcher

class RealDispatcher<PageRequestKey: Any>(
    private val actionsFlow: MutableSharedFlow<Action<PageRequestKey>>
) : Dispatcher<PageRequestKey> {
    override suspend fun dispatch(action: Action<PageRequestKey>) {
        actionsFlow.emit(action)
    }
}