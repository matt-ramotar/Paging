package org.mobilenativefoundation.storex.paging.runtime

import androidx.compose.runtime.Composable


interface UpdatingItem<ItemId : Any, ItemValue : Any> {

    @Composable
    operator fun invoke(): ItemState<ItemValue>

    suspend fun dispatch(action: Action<ItemId, ItemValue>)

    class State<ItemId : Any, ItemValue : Any>(
        val value: ItemValue?,
        val loadState: SingleLoadState
    )

    sealed interface Action<ItemId : Any, ItemValue : Any> {
        data object Refresh : Action<Nothing, Nothing>
        data object Clear : Action<Nothing, Nothing>
        data class Update<ItemId : Any, ItemValue : Any>(val value: ItemValue) :
            Action<ItemId, ItemValue>
    }
}