package org.mobilenativefoundation.storex.paging.runtime

import androidx.compose.runtime.Composable


interface UpdatingItem<ItemId : Any, ItemValue : Any> {

    @Composable
    operator fun invoke(): ItemState<ItemValue>

    suspend fun emit(event: Event<ItemId, ItemValue>)

    class State<ItemId : Any, ItemValue : Any>(
        val value: ItemValue?,
        val loadState: SingleLoadState
    )

    sealed interface Event<ItemId : Any, ItemValue : Any> {
        data object Init : Event<Nothing, Nothing>
        data object Refresh : Event<Nothing, Nothing>
        data object Clear : Event<Nothing, Nothing>
        data class Update<ItemId : Any, ItemValue : Any>(val value: ItemValue) : Event<ItemId, ItemValue>
    }
}