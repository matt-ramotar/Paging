package org.mobilenativefoundation.storex.paging.scope

import androidx.compose.runtime.Composable
import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Identifier
import org.mobilenativefoundation.storex.paging.ItemState
import org.mobilenativefoundation.storex.paging.SingleLoadState

interface UpdatingItemV2<Id : Identifier<Id>, V : Identifiable<Id>> {
    @Composable
    operator fun invoke(): ItemState<Id, V>

    class State<Id : Identifier<Id>, V : Identifiable<Id>>(
        val value: V?,
        val loadState: SingleLoadState,
    ) {
        internal var version: Long = 0
    }

    sealed interface Event<Id : Identifier<Id>, V : Identifiable<Id>> {
        data object Refresh : Event<Nothing, Nothing>
        data object Clear : Event<Nothing, Nothing>
        data class Update<Id : Identifier<Id>, V : Identifiable<Id>>(val value: V) : Event<Id, V>
    }
}