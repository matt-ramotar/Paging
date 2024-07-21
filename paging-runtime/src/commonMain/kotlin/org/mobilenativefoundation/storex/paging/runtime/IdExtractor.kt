package org.mobilenativefoundation.storex.paging.runtime

fun interface IdExtractor<ItemId : Any, ItemValue : Any> {
    fun extract(value: ItemValue): ItemId
}