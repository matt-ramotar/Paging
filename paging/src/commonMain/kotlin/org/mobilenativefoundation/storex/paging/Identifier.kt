package org.mobilenativefoundation.storex.paging


interface Measurable<in T> {
    operator fun minus(other: T): Int
}

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

interface PageKey