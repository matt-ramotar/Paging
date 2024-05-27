package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.store5.core.Identifiable

sealed interface NormalizedPagingData<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any> {
    data class Page<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        val items: List<Id>,
        val key: K,
        val prev: K?,
        val next: K?,
        val origin: DataSource,
        val extras: Map<String, String> = mapOf()
    ) : NormalizedPagingData<Id, K, V, E>

    data class Item<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
        val value: V,
        val origin: DataSource,
    ) : NormalizedPagingData<Id, K, V, E>

    enum class DataSource {
        MEMORY_CACHE,
        SOURCE_OF_TRUTH,
        NETWORK,
        PLACEHOLDER
    }

}