package org.mobilenativefoundation.storex.paging.runtime

import org.mobilenativefoundation.storex.paging.runtime.internal.pagingScope.impl.PagingScopeBuilder

interface PagingScope<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    fun getPager(): Pager<Id>
    fun getOperationManager(): OperationManager<Id, K, V>
    fun getDispatcher(): Dispatcher<K>
    fun getUpdatingItemProvider(): UpdatingItemProvider<Id, V>

    interface Builder<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
        fun build(): PagingScope<Id, K, V>
    }

    companion object {
        fun <Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> builder(
            pagingConfig: PagingConfig<Id, K>
        ): Builder<Id, K, V> {
            return PagingScopeBuilder(pagingConfig)
        }
    }
}