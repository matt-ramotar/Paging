package org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.impl

import kotlinx.coroutines.flow.MutableSharedFlow
import org.mobilenativefoundation.storex.paging.runtime.PagingAction
import org.mobilenativefoundation.storex.paging.runtime.Dispatcher

class RealDispatcher<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    private val actionsFlow: MutableSharedFlow<PagingAction<ItemId, PageRequestKey, ItemValue>>
) : Dispatcher<ItemId, PageRequestKey, ItemValue> {
    override suspend fun dispatch(action: PagingAction<ItemId, PageRequestKey, ItemValue>) {
        actionsFlow.emit(action)
    }
}