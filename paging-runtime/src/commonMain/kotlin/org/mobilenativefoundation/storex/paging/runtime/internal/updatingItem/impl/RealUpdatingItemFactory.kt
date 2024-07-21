package org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.impl

import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.UpdatingItemPresenter
import org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.api.UpdatingItemFactory

internal class RealUpdatingItemFactory<ItemId : Any, ItemValue : Any>(
    private val presenter: UpdatingItemPresenter<ItemId, ItemValue>
) : UpdatingItemFactory<ItemId, ItemValue> {
    override fun create(id: ItemId): UpdatingItem<ItemId, ItemValue> {
        return RealUpdatingItem(id, presenter)
    }

}