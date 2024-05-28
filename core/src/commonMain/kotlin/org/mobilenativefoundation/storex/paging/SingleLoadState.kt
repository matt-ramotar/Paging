package org.mobilenativefoundation.storex.paging

import kotlinx.serialization.json.JsonObject

sealed class SingleLoadState<out E : Any> {

    data object Initial : SingleLoadState<Nothing>()

    data object Loading : SingleLoadState<Nothing>()

    data object Refreshing : SingleLoadState<Nothing>()

    data object Loaded : SingleLoadState<Nothing>()

    sealed class Error<out E : Any> : SingleLoadState<E>() {

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

    data object Cleared : SingleLoadState<Nothing>()
}
