package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import org.mobilenativefoundation.storex.paging.runtime.Operation
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.MutableOperationPipeline


class RealMutableOperationPipeline<ItemId : Any, PageRequestKey : Any, ItemValue : Any>(
    private val operations: MutableList<Operation<ItemId, PageRequestKey, ItemValue>>
) : MutableOperationPipeline<ItemId, PageRequestKey, ItemValue>, MutableList<Operation<ItemId, PageRequestKey, ItemValue>> by operations