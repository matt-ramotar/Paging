package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.Operation
import org.mobilenativefoundation.storex.paging.runtime.OperationPipeline
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.RealMutableOperationPipeline

interface MutableOperationPipeline<ItemId : Any, PageRequestKey : Any, ItemValue : Any> :
    OperationPipeline<ItemId, PageRequestKey, ItemValue>, MutableList<Operation<ItemId, PageRequestKey, ItemValue>> {

    companion object {
        fun <ItemId : Any, PageRequestKey : Any, ItemValue : Any> empty(): MutableOperationPipeline<ItemId, PageRequestKey, ItemValue> {
            return RealMutableOperationPipeline(mutableListOf())
        }
    }
}



