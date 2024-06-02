package org.mobilenativefoundation.storex.paging

/**
 * Design decision to support item-level loading and item-level placeholders,
 * in comparison to [androidx.paging.ItemSnapshotList]. [androidx.paging.ItemSnapshotList]
 * has `placeholdersBefore` and `placeholdersAfter`, meaning placeholders are at the group-level.
 */
class ItemSnapshotList<Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>>(
    private val items: List<V?>,
) : AbstractList<V?>() {

    override val size: Int = items.size
    override fun get(index: Int): V? = items.getOrNull(index)

    fun getAll(): List<V?> = items

    fun getAllIds(): List<Q?> = items.map { it?.id }
}
