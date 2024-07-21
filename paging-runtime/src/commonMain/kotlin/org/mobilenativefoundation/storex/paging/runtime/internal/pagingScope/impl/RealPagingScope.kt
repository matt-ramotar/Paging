package org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.impl

import org.mobilenativefoundation.storex.paging.runtime.Dispatcher
import org.mobilenativefoundation.storex.paging.runtime.OperationManager
import org.mobilenativefoundation.storex.paging.runtime.Pager
import org.mobilenativefoundation.storex.paging.runtime.PagingScope
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItemProvider

class RealPagingScope<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    private val pager: Pager<ItemId, PageRequestKey, ItemValue>,
    private val operationManager: OperationManager<ItemId, PageRequestKey, ItemValue>,
    private val dispatcher: Dispatcher<ItemId, PageRequestKey, ItemValue>,
    private val updatingItemProvider: UpdatingItemProvider<ItemId, ItemValue>
) : PagingScope<ItemId, PageRequestKey, ItemValue> {
    override fun getPager(): Pager<ItemId, PageRequestKey, ItemValue> {
        return pager
    }

    override fun getOperationManager(): OperationManager<ItemId, PageRequestKey, ItemValue> {
        return operationManager
    }

    override fun getDispatcher(): Dispatcher<ItemId, PageRequestKey, ItemValue> {
        return dispatcher
    }

    override fun getUpdatingItemProvider(): UpdatingItemProvider<ItemId, ItemValue> {
        return updatingItemProvider
    }

}