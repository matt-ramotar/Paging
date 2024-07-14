package org.mobilenativefoundation.storex.paging.runtime

/**
 * Design decision to support item-level loading and item-level placeholders,
 * in comparison to [androidx.paging.ItemSnapshotList]. [androidx.paging.ItemSnapshotList]
 * has `placeholdersBefore` and `placeholdersAfter`, meaning placeholders are at the group-level.
 */
class ItemSnapshotList<Id : Identifier<*>, V : Identifiable<Id>>(
    private val items: List<V?>,
) : AbstractList<V?>() {

    override val size: Int = items.size
    override fun get(index: Int): V? = items.getOrNull(index)

    fun getAll(): List<V?> = items

    fun getAllIds(): List<Id?> = items.map { it?.id }
}
