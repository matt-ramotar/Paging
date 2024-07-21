package org.mobilenativefoundation.storex.paging.runtime

/**
 * Design decision to support item-level loading and item-level placeholders,
 * in comparison to [androidx.paging.ItemSnapshotList]. [androidx.paging.ItemSnapshotList]
 * has `placeholdersBefore` and `placeholdersAfter`, meaning placeholders are at the group-level.
 */
class ItemSnapshotList<ItemId : Any, ItemValue : Any>(
    private val items: List<ItemValue?>,
    private val idExtractor: IdExtractor<ItemId, ItemValue>
) : AbstractList<ItemValue?>() {

    override val size: Int = items.size
    override fun get(index: Int): ItemValue? = items.getOrNull(index)

    fun getAll(): List<ItemValue?> = items

    fun getAllIds(): List<ItemId?> = items.map { item -> item?.let { idExtractor.extract(it) } }
}
