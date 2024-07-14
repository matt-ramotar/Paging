package org.mobilenativefoundation.storex.paging.runtime

interface Identifier<T: Identifier<T>> {

    operator fun minus(other: T): Int

    operator fun compareTo(other: T): Int {
        return when {
            this - other > 0 -> 1
            this - other < 0 -> -1
            else -> 0
        }
    }
}