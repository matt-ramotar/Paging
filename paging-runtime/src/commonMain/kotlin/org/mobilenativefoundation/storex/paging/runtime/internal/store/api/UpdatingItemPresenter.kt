package org.mobilenativefoundation.storex.paging.runtime.internal.store.api

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.storex.paging.runtime.ItemState
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem

/**
 * Presenter for managing [UpdatingItem] instances.
 */
internal interface UpdatingItemPresenter<ItemId : Any, ItemValue : Any> {
    @Composable
    fun present(
        id: ItemId,
        events: Flow<UpdatingItem.Event<ItemId, ItemValue>>
    ): ItemState<ItemValue>
}