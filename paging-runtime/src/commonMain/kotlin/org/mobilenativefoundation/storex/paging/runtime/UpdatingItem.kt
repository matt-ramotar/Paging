package org.mobilenativefoundation.storex.paging.runtime

import androidx.compose.runtime.Composable


fun interface UpdatingItem<Id : Identifier<Id>, V : Identifiable<Id>> {
    @Composable
    operator fun invoke(): ItemState<Id, V>

    class State<Id : Identifier<Id>, V : Identifiable<Id>>(
        val value: V?,
        val loadState: SingleLoadState,
        val eventSink: (event: Event<Id, V>) -> Unit
    ) {
        internal var version: Long = 0
    }

    sealed interface Event<Id : Identifier<Id>, V : Identifiable<Id>> {
        data object Refresh : Event<Nothing, Nothing>
        data object Clear : Event<Nothing, Nothing>
        data class Update<Id : Identifier<Id>, V : Identifiable<Id>>(val value: V) : Event<Id, V>
    }
}