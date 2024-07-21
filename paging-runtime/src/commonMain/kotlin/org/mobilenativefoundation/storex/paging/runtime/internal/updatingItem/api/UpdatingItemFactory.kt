package org.mobilenativefoundation.storex.paging.runtime.internal.updatingItem.api

import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem

internal interface UpdatingItemFactory<ItemId : Any, ItemValue : Any> {
    fun create(id: ItemId): UpdatingItem<ItemId, ItemValue>
}