package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.store5.core.Identifiable

interface PagingPrefetcher<Id : Comparable<Id>, V : Identifiable<Id>> {
    fun prefetch()
}