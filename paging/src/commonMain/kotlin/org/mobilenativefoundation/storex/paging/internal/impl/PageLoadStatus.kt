package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.serialization.json.JsonObject
import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.ItemSnapshotList
import org.mobilenativefoundation.storex.paging.Quantifiable


sealed interface PageLoadStatus<Id : Comparable<Id>, Q : Quantifiable<Id>, out K : Any, out V : Identifiable<Id, Q>, out E : Any> {

    val isTerminal: Boolean

    data class Processing<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
        override val isTerminal: Boolean = false
    ) : PageLoadStatus<Id, Q, K, V, E>

    data class Loading<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
        override val isTerminal: Boolean = false,
        val source: Source
    ) : PageLoadStatus<Id, Q, K, V, E> {
        enum class Source {
            MemoryCache,
            Database,
            Remote
        }

        companion object {
            fun <Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> memoryCache() =
                Loading<Id, Q, K, V, E>(
                    source = Source.MemoryCache
                )

            fun <Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> database() =
                Loading<Id, Q, K, V, E>(
                    source = Source.Database
                )

            fun <Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> remote() =
                Loading<Id, Q, K, V, E>(
                    source = Source.Remote
                )
        }
    }


    data class SkippingLoad<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
        val reason: Reason,
        override val isTerminal: Boolean = true
    ) : PageLoadStatus<Id, Q, K, V, E> {
        enum class Reason {
            AlreadyInFlight
        }

        companion object {
            fun <Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> inFlight() =
                SkippingLoad<Id, Q, K, V, E>(
                    Reason.AlreadyInFlight
                )
        }
    }

    data class Success<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
        override val isTerminal: Boolean = true,
        val snapshot: ItemSnapshotList<Id, Q, V>,
        val prevKey: K? = null,
        val nextKey: K? = null,
        val source: Source
    ) : PageLoadStatus<Id, Q, K, V, E> {
        enum class Source {
            MemoryCache,
            Database,
            Network
        }
    }

    data class Error<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
        val error: E,
        val extras: JsonObject? = null,
        override val isTerminal: Boolean
    ) : PageLoadStatus<Id, Q, K, V, E>

    data class Empty<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
        override val isTerminal: Boolean = true,
        val reason: Reason
    ) : PageLoadStatus<Id, Q, K, V, E> {
        enum class Reason {
            LocalOnlyRequest
        }
    }

}