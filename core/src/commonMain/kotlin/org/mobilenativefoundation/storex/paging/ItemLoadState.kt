package org.mobilenativefoundation.storex.paging

sealed class ItemLoadState<out E: Any> {

    data object Initial : ItemLoadState<Nothing>()

    data object Loading : ItemLoadState<Nothing>()

    data object Loaded : ItemLoadState<Nothing>()

    data class Error<E: Any>(
        val error: E,
        val extras: Map<String, Any> = mapOf()
    ) : ItemLoadState<E>()
}
