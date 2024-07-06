package org.mobilenativefoundation.storex.paging.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.custom.LaunchEffect
import org.mobilenativefoundation.storex.paging.custom.SideEffect

interface PagingScope<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>> {
    fun getPager(): Pager<Id>
    fun getOperationManager(): OperationManager<Id, K, V>
    fun getDispatcher(): Dispatcher<K>
    fun getUpdatingItemProvider(): UpdatingItemProvider<Id, V>

    class Builder<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
        private val pagingConfig: PagingConfig<Id, K>
    ) {

        private var initialLoadParams = PagingSource.LoadParams(
            pagingConfig.initialKey,
            strategy = LoadStrategy.SkipCache,
            direction = LoadDirection.Append
        )

        private val actionsFlow: MutableSharedFlow<Action<K>> = MutableSharedFlow(replay = 20)

        private var storexPagingSource: PagingSource<Id, K, V>? = null
        private var androidxPagingSource: PagingSource<Id, K, V>? = null

        private var coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
        private val launchEffects: MutableList<LaunchEffect> = mutableListOf()
        private val sideEffects: MutableList<SideEffect<Id, V>> = mutableListOf()
        private var initialFetchingState: FetchingState<Id, K> = FetchingState()
        private var fetchingStrategy: FetchingStrategy<Id, K> = TODO()

        fun build(): PagingScope<Id, K, V> {
            TODO()
        }
    }
}