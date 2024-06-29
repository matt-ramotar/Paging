package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

// TODO: Design decision to not have each item update pass down UI tree to optimize recompositions only recompose the item
// So each item needs its own presenter
class SelfUpdatingItem<Id : Identifier<*>, V : Identifiable<Id>>(
    private val presenter: @Composable (events: Flow<Event<Id, V>>) -> ItemState<Id, V>
) {
    private val _events = MutableSharedFlow<Event<Id, V>>(extraBufferCapacity = 20)

    @Composable
    operator fun invoke() = presenter(_events)

    suspend fun emit(event: Event<Id, V>) {
        _events.emit(event)
    }

    sealed interface Event<Id : Identifier<*>, V : Identifiable<Id>> {

        data class Refresh<Id : Identifier<*>, V : Identifiable<Id>>(
            val message: String? = null,
        ) : Event<Id, V>

        data class Update<Id : Identifier<*>, V : Identifiable<Id>>(
            val value: V,
        ) : Event<Id, V>

        data class Clear<Id : Identifier<*>, V : Identifiable<Id>>(
            val message: String? = null
        ) : Event<Id, V>
    }
}