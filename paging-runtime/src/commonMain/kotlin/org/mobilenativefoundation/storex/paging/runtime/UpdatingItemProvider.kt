package org.mobilenativefoundation.storex.paging.runtime


interface UpdatingItemProvider<ItemId : Any, ItemValue : Any> {
    suspend fun get(id: ItemId): UpdatingItem<ItemId, ItemValue>
    suspend fun remove(id: ItemId)
    suspend fun clear()
}
