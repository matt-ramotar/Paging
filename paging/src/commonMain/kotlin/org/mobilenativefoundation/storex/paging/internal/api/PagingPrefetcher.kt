package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Identifiable

interface PagingPrefetcher<Id : Comparable<Id>, V : Identifiable<Id>> {
    fun prefetch()
}