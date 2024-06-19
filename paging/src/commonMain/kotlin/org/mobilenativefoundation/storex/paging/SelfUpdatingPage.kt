package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class SelfUpdatingPage<Id : Identifier<*>, V: Identifiable<Id>>(
    private val presenter: @Composable (events: Flow<Event<Id>>) -> PageState<Id, V>
) {

    private val _events = MutableSharedFlow<Event<Id>>()

    @Composable
    operator fun invoke(): PageState<Id, V> = presenter(_events)

    suspend fun emit(event: Event<Id>) {
        _events.emit(event)
    }

    sealed interface Event<Id : Identifier<*>> {

        data class Refresh<Id : Identifier<*>>(
            val message: String? = null,
        ) : Event<Id>

        data class Clear<Id : Identifier<*>>(
            val message: String? = null
        ) : Event<Id>
    }
}