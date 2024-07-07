package org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.impl

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableSharedFlow
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.ItemState
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.UpdatingItemPresenter

internal class RealUpdatingItem<Id : Identifier<Id>, V : Identifiable<Id>>(
    private val id: Id,
    private val presenter: UpdatingItemPresenter<Id, V>
) : UpdatingItem<Id, V> {

    private val _events = MutableSharedFlow<UpdatingItem.Event<Id, V>>(extraBufferCapacity = 20)

    @Composable
    override fun invoke(): ItemState<Id, V> {
        return presenter.present(id, _events)
    }

    override suspend fun emit(event: UpdatingItem.Event<Id, V>) {
        _events.emit(event)
    }
}