package org.mobilenativefoundation.storex.paging.test.utils.models

import kotlinx.serialization.Serializable
import org.mobilenativefoundation.storex.paging.Quantifiable

@Serializable
data class PostId(override val value: String) : Quantifiable<String> {
    override fun minus(other: Quantifiable<String>): Int {

        return if (value == PLACEHOLDER && other.value == PLACEHOLDER) {
            // both ids are placeholders
            0
        } else if (value == PLACEHOLDER) {
            // id is placeholder
            // [1, placeholder]
            Int.MIN_VALUE

        } else if (other.value == PLACEHOLDER) {
            // other id is placeholder
            // [placeholder, 1]
            value.toInt()

        } else {
            // neither ids are placeholders
            value.toInt() - other.value.toInt()
        }

    }

    companion object {
        private const val PLACEHOLDER = "-1"
        val Placeholder = PostId(PLACEHOLDER)
    }

    override fun plus(other: Quantifiable<String>): Int {
        TODO("Not yet implemented")
    }

    override fun times(other: Quantifiable<String>): Int {
        TODO("Not yet implemented")
    }

    override fun div(other: Quantifiable<String>): Int {
        TODO("Not yet implemented")
    }
}