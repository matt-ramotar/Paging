package org.mobilenativefoundation.storex.paging

import kotlinx.serialization.Serializable


interface Quantifiable<T : Comparable<T>> {
    val value: T
    operator fun plus(other: Quantifiable<T>): Int
    operator fun minus(other: Quantifiable<T>): Int
    operator fun times(other: Quantifiable<T>): Int
    operator fun div(other: Quantifiable<T>): Int
}


@Serializable
data class IntId(override val value: Int) : Quantifiable<Int> {
    override operator fun plus(other: Quantifiable<Int>): Int = value + other.value
    override operator fun minus(other: Quantifiable<Int>): Int = value - other.value
    override operator fun times(other: Quantifiable<Int>): Int = value * other.value
    override operator fun div(other: Quantifiable<Int>): Int = value / other.value
}

