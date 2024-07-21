package org.mobilenativefoundation.storex.paging.runtime

interface OperationManager<ItemId: Any, PageRequestKey: Any, ItemValue: Any> {
    suspend fun add(operation: Operation<ItemId, PageRequestKey, ItemValue>)
    suspend fun remove(operation: Operation<ItemId, PageRequestKey, ItemValue>)
    suspend fun removeAll(predicate: (Operation<ItemId, PageRequestKey, ItemValue>) -> Boolean)
    suspend fun clear()
    fun get(): List<Operation<ItemId, PageRequestKey, ItemValue>>
    fun get(predicate: (Operation<ItemId, PageRequestKey, ItemValue>) -> Boolean): List<Operation<ItemId, PageRequestKey, ItemValue>>
}