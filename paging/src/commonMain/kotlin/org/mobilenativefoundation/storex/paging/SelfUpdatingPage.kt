package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class SelfUpdatingPage<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    private val presenter: @Composable (events: Flow<Event<Id, Q, K, V, E>>) -> PageState<Id, Q, K, V, E>
) {

    private val _events = MutableSharedFlow<Event<Id, Q, K, V, E>>()

    @Composable
    operator fun invoke(): PageState<Id, Q, K, V, E> = presenter(_events)

    suspend fun emit(event: Event<Id, Q, K, V, E>) {
        _events.emit(event)
    }

    sealed interface Event<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any> {

        data class Refresh<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
            val message: String? = null,
        ) : Event<Id, Q, K, V, E>

        data class Clear<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
            val message: String? = null
        ) : Event<Id, Q, K, V, E>
    }
}