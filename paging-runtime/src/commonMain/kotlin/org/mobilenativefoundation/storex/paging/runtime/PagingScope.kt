package org.mobilenativefoundation.storex.paging.runtime

interface PagingScope<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    fun getPager(): Pager<Id>
    fun getOperationManager(): OperationManager<Id, K, V>
    fun getDispatcher(): Dispatcher<K>
    fun getUpdatingItemProvider(): UpdatingItemProvider<Id, V>

    class Builder<Id: Identifier<Id>, K: Comparable<K>, V: Identifiable<Id>>(
        private val pagingConfig: PagingConfig<Id, K>
    ) {

        private var initialLoadParams = PagingSource.LoadParams(
            pagingConfig.initialKey,
            strategy = LoadStrategy.SkipCache,
            direction = LoadDirection.Append
        )



        fun build(): PagingScope<Id, K, V> {
            TODO()
        }
    }
}