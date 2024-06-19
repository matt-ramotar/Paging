package org.mobilenativefoundation.storex.paging.internal.impl

import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList


sealed interface PageLoadState<Id : Identifier<*>, out K : Any, out V : Identifiable<Id>> {

    val isTerminal: Boolean

    data class Processing<Id : Identifier<*>, K : Any, V : Identifiable<Id>>(
        override val isTerminal: Boolean = false
    ) : PageLoadState<Id, K, V>

    data class Loading<Id : Identifier<*>, K : Any, V : Identifiable<Id>>(
        override val isTerminal: Boolean = false,
        val source: Source
    ) : PageLoadState<Id, K, V> {
        enum class Source {
            MemoryCache,
            Database,
            Remote
        }

        companion object {
            fun <Id : Identifier<*>, K : Any, V : Identifiable<Id>> memoryCache() =
                Loading<Id, K, V>(
                    source = Source.MemoryCache
                )

            fun <Id : Identifier<*>, K : Any, V : Identifiable<Id>> database() =
                Loading<Id, K, V>(
                    source = Source.Database
                )

            fun <Id : Identifier<*>, K : Any, V : Identifiable<Id>> remote() =
                Loading<Id, K, V>(
                    source = Source.Remote
                )
        }
    }


    data class SkippingLoad<Id : Identifier<*>, K : Any, V : Identifiable<Id>>(
        val reason: Reason,
        override val isTerminal: Boolean = true
    ) : PageLoadState<Id, K, V> {
        enum class Reason {
            AlreadyInFlight
        }

        companion object {
            fun <Id : Identifier<*>, K : Any, V : Identifiable<Id>> inFlight() =
                SkippingLoad<Id, K, V>(
                    Reason.AlreadyInFlight
                )
        }
    }

    data class Success<Id : Identifier<*>, K : Any, V : Identifiable<Id>>(
        override val isTerminal: Boolean = true,
        val snapshot: ItemSnapshotList<Id, V>,
        val prevKey: K? = null,
        val nextKey: K? = null,
        val source: Source
    ) : PageLoadState<Id, K, V> {
        enum class Source {
            MemoryCache,
            Database,
            Network
        }
    }

    sealed class Error<Id : Identifier<*>, K : Any, V : Identifiable<Id>> : PageLoadState<Id, K, V> {
        data class Message<Id : Identifier<*>, K : Any, V : Identifiable<Id>>(
            val error: String,
            override val isTerminal: Boolean
        ) : Error<Id, K, V>()

        data class Exception<Id : Identifier<*>, K : Any, V : Identifiable<Id>>(
            val error: Throwable,
            override val isTerminal: Boolean
        ) : Error<Id, K, V>()
    }

    data class Empty<Id : Identifier<*>, K : Any, V : Identifiable<Id>>(
        override val isTerminal: Boolean = true,
        val reason: Reason
    ) : PageLoadState<Id, K, V> {
        enum class Reason {
            LocalOnlyRequest,
            NetworkResponse
        }
    }

}