package org.mobilenativefoundation.storex.paging.utils.timeline.models

import org.mobilenativefoundation.storex.paging.custom.ErrorFactory


sealed interface TimelineError {
    data class Exception(val throwable: Throwable) : TimelineError
    data class Message(val message: String) : TimelineError

    class Factory : ErrorFactory<TimelineError> {
        override fun create(throwable: Throwable): TimelineError =
            TimelineError.Exception(throwable)

        override fun create(message: String): TimelineError {
            TODO("Not yet implemented")
        }
    }
}