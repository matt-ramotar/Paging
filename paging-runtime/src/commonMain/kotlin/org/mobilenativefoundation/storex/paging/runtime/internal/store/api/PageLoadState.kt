package org.mobilenativefoundation.storex.paging.runtime.internal.store.api

import org.mobilenativefoundation.storex.paging.runtime.ItemSnapshotList

internal sealed interface PageLoadState<ItemId : Any, out PageRequestKey : Any, out ItemValue : Any> {

    val isTerminal: Boolean

    data class Processing<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
        override val isTerminal: Boolean = false
    ) : PageLoadState<ItemId, PageRequestKey, ItemValue>

    data class Loading<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
        override val isTerminal: Boolean = false,
        val source: Source
    ) : PageLoadState<ItemId, PageRequestKey, ItemValue> {
        enum class Source {
            MemoryCache,
            Database,
            Remote
        }

        companion object {
            fun <ItemId : Any, PageRequestKey : Any, ItemValue : Any> memoryCache() =
                Loading<ItemId, PageRequestKey, ItemValue>(
                    source = Source.MemoryCache
                )

            fun <ItemId : Any, PageRequestKey : Any, ItemValue : Any> database() =
                Loading<ItemId, PageRequestKey, ItemValue>(
                    source = Source.Database
                )

            fun <ItemId : Any, PageRequestKey : Any, ItemValue : Any> remote() =
                Loading<ItemId, PageRequestKey, ItemValue>(
                    source = Source.Remote
                )
        }
    }


    data class SkippingLoad<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
        val reason: Reason,
        override val isTerminal: Boolean = true
    ) : PageLoadState<ItemId, PageRequestKey, ItemValue> {
        enum class Reason {
            AlreadyInFlight
        }

        companion object {
            fun <ItemId : Any, PageRequestKey : Any, ItemValue : Any> inFlight() =
                SkippingLoad<ItemId, PageRequestKey, ItemValue>(
                    Reason.AlreadyInFlight
                )
        }
    }

    data class Success<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
        override val isTerminal: Boolean = true,
        val snapshot: ItemSnapshotList<ItemId, ItemValue>,
        val prevKey: PageRequestKey? = null,
        val nextKey: PageRequestKey? = null,
        val source: Source
    ) : PageLoadState<ItemId, PageRequestKey, ItemValue> {
        enum class Source {
            MemoryCache,
            Database,
            Network
        }
    }

    sealed class Error<ItemId : Any, PageRequestKey : Any, ItemValue : Any> :
        PageLoadState<ItemId, PageRequestKey, ItemValue> {
        data class Message<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
            val error: String,
            override val isTerminal: Boolean
        ) : Error<ItemId, PageRequestKey, ItemValue>()

        data class Exception<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
            val error: Throwable,
            override val isTerminal: Boolean
        ) : Error<ItemId, PageRequestKey, ItemValue>()
    }

    data class Empty<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
        override val isTerminal: Boolean = true,
        val reason: Reason
    ) : PageLoadState<ItemId, PageRequestKey, ItemValue> {
        enum class Reason {
            LocalOnlyRequest,
            NetworkResponse
        }
    }

}