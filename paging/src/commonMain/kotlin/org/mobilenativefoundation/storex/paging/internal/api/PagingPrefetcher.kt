package org.mobilenativefoundation.storex.paging.internal.api

import org.mobilenativefoundation.storex.paging.Identifiable
import org.mobilenativefoundation.storex.paging.Quantifiable

interface PagingPrefetcher<Id : Comparable<Id>, Q: Quantifiable<Id>, V : Identifiable<Id, Q>> {
    fun prefetch()
}