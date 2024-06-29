@file:OptIn(InternalSerializationApi::class)

package org.mobilenativefoundation.storex.paging

import kotlinx.serialization.InternalSerializationApi

interface SelfUpdatingItemFactory<Id : Identifier<Id>, V : Identifiable<Id>> {
    fun createSelfUpdatingItem(id: Id): SelfUpdatingItem<Id, V>
}


sealed class PagingRequest<out K : Any> {

    data class ProcessQueue internal constructor(
        val direction: LoadDirection
    ) : PagingRequest<Nothing>()

    data class SkipQueue<K : Any> internal constructor(
        val key: K,
        val direction: LoadDirection,
        val strategy: LoadStrategy,
    ) : PagingRequest<K>()

    data class Enqueue<K : Any> internal constructor(
        val key: K,
        val direction: LoadDirection,
        val strategy: LoadStrategy,
        val jump: Boolean
    ) : PagingRequest<K>()

    data object Invalidate : PagingRequest<Nothing>()

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