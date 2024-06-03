@file:Suppress("UNCHECKED_CAST")

package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// TODO: Design decision to not have each item update pass down UI tree to optimize recompositions only recompose the item
// So each item needs its own presenter
class SelfUpdatingItem<Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any>(
    private val presenter: @Composable (events: Flow<Event<Id, Q, V, E>>) -> ItemState<Id, Q, V, E>
) {
    private val _events = MutableSharedFlow<Event<Id, Q, V, E>>(extraBufferCapacity = 20)

    @Composable
    operator fun invoke() = presenter(_events)

    suspend fun emit(event: Event<Id, Q, V, E>) {
        _events.emit(event)
    }

    sealed interface Event<Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> {

        data class Refresh<Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any>(
            val message: String? = null,
        ) : Event<Id, Q, V, E>

        data class Update<Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any>(
            val value: V,
        ) : Event<Id, Q, V, E>

        data class Clear<Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any>(
            val message: String? = null
        ) : Event<Id, Q, V, E>
    }
}