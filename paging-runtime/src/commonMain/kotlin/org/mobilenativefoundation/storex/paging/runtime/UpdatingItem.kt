package org.mobilenativefoundation.storex.paging.runtime

import androidx.compose.runtime.Composable


interface UpdatingItem<Id : Identifier<Id>, V : Identifiable<Id>> {

    @Composable
    operator fun invoke(): ItemState<Id, V>

    suspend fun emit(event: Event<Id, V>)

    class State<Id : Identifier<Id>, V : Identifiable<Id>>(
        val value: V?,
        val loadState: SingleLoadState
    )

    sealed interface Event<Id : Identifier<Id>, V : Identifiable<Id>> {
        data object Init: Event<Nothing, Nothing>
        data object Refresh : Event<Nothing, Nothing>
        data object Clear : Event<Nothing, Nothing>
        data class Update<Id : Identifier<Id>, V : Identifiable<Id>>(val value: V) : Event<Id, V>
    }
}