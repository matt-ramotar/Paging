package org.mobilenativefoundation.storex.paging.runtime

interface Comparator<T> {
    /**
     * Compares [a] with [b] for order.
     * Returns zero if [a] is equal to [b], a negative number if [a] is less [b], or a positive number if [a] is greater than [b].
     */
    fun compare(a: T, b: T): Int

    /**
     * Determines the absolute distance between [a] and [b].
     */
    fun distance(a: T, b: T): Int?
}

object StringComparator : Comparator<String> {
    override fun compare(a: String, b: String): Int = a.compareTo(b)

    override fun distance(a: String, b: String): Int? {
        // No default distance operation for strings
        return null
    }
}

object LongComparator : Comparator<Long> {
    override fun compare(a: Long, b: Long): Int = a.compareTo(b)
    override fun distance(a: Long, b: Long): Int = (a - b).toInt()
}

object IntComparator : Comparator<Int> {
    override fun compare(a: Int, b: Int): Int = a.compareTo(b)

    override fun distance(a: Int, b: Int): Int = a - b
}