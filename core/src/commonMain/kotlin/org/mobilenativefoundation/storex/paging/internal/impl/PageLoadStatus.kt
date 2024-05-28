package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.serialization.json.JsonObject
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList


sealed interface PageLoadStatus<Id : Comparable<Id>, out K : Any, out V : Identifiable<Id>, out E : Any> {

    val isTerminal: Boolean

    data class Processing<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        override val isTerminal: Boolean = false
    ) : PageLoadStatus<Id, K, V, E>

    data class Loading<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        override val isTerminal: Boolean = false,
        val source: Source
    ) : PageLoadStatus<Id, K, V, E> {
        enum class Source {
            MemoryCache,
            Database,
            Remote
        }

        companion object {
            fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> memoryCache() =
                Loading<Id, K, V, E>(
                    source = Source.MemoryCache
                )

            fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> database() =
                Loading<Id, K, V, E>(
                    source = Source.Database
                )

            fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> remote() =
                Loading<Id, K, V, E>(
                    source = Source.Remote
                )
        }
    }


    data class SkippingLoad<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        val reason: Reason,
        override val isTerminal: Boolean = true
    ) : PageLoadStatus<Id, K, V, E> {
        enum class Reason {
            AlreadyInFlight
        }

        companion object {
            fun <Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> inFlight() =
                SkippingLoad<Id, K, V, E>(
                    Reason.AlreadyInFlight
                )
        }
    }

    data class Success<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        override val isTerminal: Boolean = true,
        val snapshot: ItemSnapshotList<Id, V>,
        val prevKey: K? = null,
        val nextKey: K? = null,
        val source: Source
    ) : PageLoadStatus<Id, K, V, E> {
        enum class Source {
            MemoryCache,
            Database,
            Network
        }
    }

    data class Error<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        val error: E,
        val extras: JsonObject? = null,
        override val isTerminal: Boolean
    ) : PageLoadStatus<Id, K, V, E>

    data class Empty<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        override val isTerminal: Boolean = true,
        val reason: Reason
    ) : PageLoadStatus<Id, K, V, E> {
        enum class Reason {
            LocalOnlyRequest
        }
    }

}