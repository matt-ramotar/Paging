package org.mobilenativefoundation.storex.paging.test.utils.models

import kotlinx.serialization.Serializable
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory

@Serializable
sealed interface TimelineError {
    data class Exception(val throwable: Throwable) : TimelineError
    data class Message(val message: String) : TimelineError

    class Factory : ErrorFactory<TimelineError> {
        override fun create(throwable: Throwable): TimelineError =
            Exception(throwable)

        override fun create(message: String): TimelineError {
            TODO("Not yet implemented")
        }
    }
}