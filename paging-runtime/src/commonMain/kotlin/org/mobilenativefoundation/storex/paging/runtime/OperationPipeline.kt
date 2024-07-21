package org.mobilenativefoundation.storex.paging.runtime

interface OperationPipeline<ItemId: Any, PageRequestKey: Any, ItemValue: Any>: List<Operation<ItemId, PageRequestKey, ItemValue>>