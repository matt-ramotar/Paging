package org.mobilenativefoundation.storex.paging

import kotlinx.serialization.json.JsonObject

sealed class SingleLoadState {

    data object Initial : SingleLoadState()

    data object Loading : SingleLoadState()

    data object Refreshing : SingleLoadState()

    data object Loaded : SingleLoadState()

    sealed class Error<out E : Any> : SingleLoadState() {

        abstract val error: E
        abstract val extras: JsonObject?

        data class InitialLoad<E : Any>(
            override val error: E,
            override val extras: JsonObject? = null
        ) : Error<E>()

        data class Refreshing<E : Any>(
            override val error: E,
            override val extras: JsonObject? = null
        ) : Error<E>()
    }

    data object Cleared : SingleLoadState()
}
