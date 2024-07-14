package org.mobilenativefoundation.storex.paging.runtime

sealed interface LoadStrategy {
    data class CacheFirst(val alsoLoadFromNetwork: Boolean) : LoadStrategy

    data object SkipCache : LoadStrategy

    data object LocalOnly : LoadStrategy
}