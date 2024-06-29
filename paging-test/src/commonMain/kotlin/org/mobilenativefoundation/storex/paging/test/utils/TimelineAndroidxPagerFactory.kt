package org.mobilenativefoundation.storex.paging.test.utils

import androidx.paging.Pager
import androidx.paging.PagingConfig
import org.mobilenativefoundation.storex.paging.test.utils.api.TimelineApi
import org.mobilenativefoundation.storex.paging.test.utils.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.Post
import org.mobilenativefoundation.storex.paging.utils.timeline.server.Server

class TimelineAndroidxPagerFactory(
    private val pageSize: Int = 20,
    private val prefetchDistance: Int = 100,
) {
    private val api = TimelineApi(Server())

    fun create(): Pager<GetFeedRequest, Post> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, prefetchDistance = prefetchDistance),
            pagingSourceFactory = { TimelineAndroidxPagingSource(api, pageSize) }
        )
    }
}