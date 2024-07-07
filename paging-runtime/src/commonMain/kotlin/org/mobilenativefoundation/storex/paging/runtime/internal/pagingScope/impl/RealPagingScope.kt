package org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.impl

import org.mobilenativefoundation.storex.paging.runtime.Dispatcher
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.OperationManager
import org.mobilenativefoundation.storex.paging.runtime.Pager
import org.mobilenativefoundation.storex.paging.runtime.PagingScope
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItemProvider

class RealPagingScope<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val pager: Pager<Id>,
    private val operationManager: OperationManager<Id, K, V>,
    private val dispatcher: Dispatcher<K>,
    private val updatingItemProvider: UpdatingItemProvider<Id, V>
) : PagingScope<Id, K, V> {
    override fun getPager(): Pager<Id> {
        return pager
    }

    override fun getOperationManager(): OperationManager<Id, K, V> {
        return operationManager
    }

    override fun getDispatcher(): Dispatcher<K> {
        return dispatcher
    }

    override fun getUpdatingItemProvider(): UpdatingItemProvider<Id, V> {
        return updatingItemProvider
    }

}