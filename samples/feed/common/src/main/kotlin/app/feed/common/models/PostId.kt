package app.feed.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.mobilenativefoundation.storex.paging.Identifier

@Serializable
@Parcelize
data class PostId(override val value: String) : Identifier<String>, Parcelable {
    override fun minus(other: Identifier<String>): Int {

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

    override fun plus(other: Identifier<String>): Int {
        TODO("Not yet implemented")
    }

    override fun times(other: Identifier<String>): Int {
        TODO("Not yet implemented")
    }

    override fun div(other: Identifier<String>): Int {
        TODO("Not yet implemented")
    }
}