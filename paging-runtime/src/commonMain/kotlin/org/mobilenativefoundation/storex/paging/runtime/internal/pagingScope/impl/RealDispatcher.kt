package org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.impl

import kotlinx.coroutines.flow.MutableSharedFlow
import org.mobilenativefoundation.storex.paging.runtime.Action
import org.mobilenativefoundation.storex.paging.runtime.Dispatcher

class RealDispatcher<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    private val actionsFlow: MutableSharedFlow<Action<ItemId, PageRequestKey, ItemValue>>
) : Dispatcher<ItemId, PageRequestKey, ItemValue> {
    override suspend fun dispatch(action: Action<ItemId, PageRequestKey, ItemValue>) {
        actionsFlow.emit(action)
    }
}