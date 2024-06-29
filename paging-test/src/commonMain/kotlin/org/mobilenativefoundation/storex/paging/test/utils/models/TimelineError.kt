package org.mobilenativefoundation.storex.paging.test.utils.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory

@Serializable
sealed interface TimelineError {
    @Serializable
    data class Exception(val throwable: String) : TimelineError
    @Serializable
    data class Message(val message: String) : TimelineError

    class Factory : ErrorFactory<TimelineError> {
        override fun create(throwable: Throwable): TimelineError =
            Exception(throwable.message.orEmpty())

        override fun create(message: String): TimelineError =
            Message(message)
    }
}