package org.mobilenativefoundation.storex.paging.runtime

sealed class PagingAction<out ItemId : Any, out PageRequestKey : Any, out ItemValue : Any> {

    data class ProcessQueue internal constructor(
        val direction: LoadDirection
    ) : PagingAction<Nothing, Nothing, Nothing>()

    data class SkipQueue<K : Any> internal constructor(
        val key: K,
        val direction: LoadDirection,
        val strategy: LoadStrategy,
    ) : PagingAction<Nothing, K, Nothing>()

    data class Enqueue<K : Any> internal constructor(
        val key: K,
        val direction: LoadDirection,
        val strategy: LoadStrategy,
        val jump: Boolean
    ) : PagingAction<Nothing, K, Nothing>()

    data object Invalidate : PagingAction<Nothing, Nothing, Nothing>()

    data class AddOperation<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
        val operation: Operation<ItemId, PageRequestKey, ItemValue>
    ) : PagingAction<ItemId, PageRequestKey, ItemValue>()

    data class RemoveOperation<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
        val operation: Operation<ItemId, PageRequestKey, ItemValue>
    ) : PagingAction<ItemId, PageRequestKey, ItemValue>()

    companion object {
        fun <K : Any> skipQueue(
            key: K,
            direction: LoadDirection,
            strategy: LoadStrategy = LoadStrategy.SkipCache
        ) =
            SkipQueue(key, direction, strategy)

        fun processQueue(direction: LoadDirection) = ProcessQueue(direction)

        fun <K : Any> enqueue(key: K, direction: LoadDirection, jump: Boolean, strategy: LoadStrategy = LoadStrategy.SkipCache) =
            Enqueue(key, direction, strategy, jump)

        fun invalidate() = Invalidate

        fun <K : Any> append(key: K, jump: Boolean = false, strategy: LoadStrategy = LoadStrategy.SkipCache) =
            Enqueue(key, LoadDirection.Append, strategy, jump)

        fun <K : Any> prepend(key: K, jump: Boolean = false, strategy: LoadStrategy = LoadStrategy.SkipCache) =
            Enqueue(key, LoadDirection.Prepend, strategy, jump)
    }
}