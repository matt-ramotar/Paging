package org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.impl

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableSharedFlow
import org.mobilenativefoundation.storex.paging.runtime.ItemState
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.UpdatingItemPresenter

internal class RealUpdatingItem<ItemId : Any, ItemValue : Any>(
    private val id: ItemId,
    private val presenter: UpdatingItemPresenter<ItemId, ItemValue>
) : UpdatingItem<ItemId, ItemValue> {

    private val _events = MutableSharedFlow<UpdatingItem.Action<ItemId, ItemValue>>(extraBufferCapacity = 20)

    @Composable
    override fun invoke(): ItemState<ItemValue> {
        return presenter.present(id, _events)
    }

    override suspend fun dispatch(action: UpdatingItem.Action<ItemId, ItemValue>) {
        _events.emit(action)
    }
}