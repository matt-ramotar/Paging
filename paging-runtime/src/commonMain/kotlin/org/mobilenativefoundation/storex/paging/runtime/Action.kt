package org.mobilenativefoundation.storex.paging.runtime

import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.MutableOperationPipeline

sealed class Action<out ItemId: Any, out PageRequestKey : Any, out ItemValue: Any> {

    data class ProcessQueue internal constructor(
        val direction: LoadDirection
    ) : Action<Nothing, Nothing, Nothing>()

    data class SkipQueue<K : Any> internal constructor(
        val key: K,
        val direction: LoadDirection,
        val strategy: LoadStrategy,
    ) : Action<Nothing, K, Nothing>()

    data class Enqueue<K : Any> internal constructor(
        val key: K,
        val direction: LoadDirection,
        val strategy: LoadStrategy,
        val jump: Boolean
    ) : Action<Nothing, K, Nothing>()

    data object Invalidate : Action<Nothing, Nothing, Nothing>()

    data class AddOperation<ItemId: Any, PageRequestKey: Any, ItemValue: Any>(
        val operation: Operation<ItemId, PageRequestKey, ItemValue>
    ) : Action<ItemId, PageRequestKey, ItemValue>()

    data class RemoveOperation<ItemId: Any, PageRequestKey: Any, ItemValue: Any>(
        val operation: Operation<ItemId, PageRequestKey, ItemValue>
    ) : Action<ItemId, PageRequestKey, ItemValue>()

    data object ClearOperations : Action<Nothing, Nothing, Nothing>()

    companion object {
        fun <K : Any> skipQueue(
            key: K,
            direction: LoadDirection,
            strategy: LoadStrategy = LoadStrategy.SkipCache
        ) =
            SkipQueue(key, direction, strategy)

        fun processQueue(direction: LoadDirection) = ProcessQueue(direction)
        fun <K : Any> enqueue(key: K, jump: Boolean, strategy: LoadStrategy = LoadStrategy.SkipCache) =
            Enqueue(key, LoadDirection.Append, strategy, jump)

        fun invalidate() = Invalidate
    }
}