package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class SelfUpdatingPage<Id : Comparable<Id>, K: Any, V : Identifiable<Id>, E : Any>(
    private val presenter: @Composable (events: Flow<Event<Id, K, V, E>>) -> PageState<Id, K, V, E>
) {

    private val _events = MutableSharedFlow<Event<Id, K, V, E>>()
    @Composable
    operator fun invoke(): PageState<Id, K, V, E> = presenter(_events)

    suspend fun emit(event: Event<Id, K, V, E>) {
        _events.emit(event)
    }

    sealed interface Event<Id : Comparable<Id>, K: Any, V : Identifiable<Id>, E : Any> {

        data class Refresh<Id : Comparable<Id>,K: Any, V : Identifiable<Id>, E : Any>(
            val message: String? = null,
        ) : Event<Id, K, V, E>

        data class Clear<Id : Comparable<Id>,K: Any, V : Identifiable<Id>, E : Any>(
            val message: String? = null
        ) : Event<Id, K, V, E>
    }
}