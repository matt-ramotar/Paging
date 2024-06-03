package app.feed.androidx

import androidx.paging.Pager
import androidx.paging.PagingConfig
import app.feed.common.TimelineAndroidxPagingSource
import app.feed.common.api.TimelineApi
import app.feed.common.models.GetFeedRequest
import app.feed.common.models.Post
import app.feed.common.server.Server

class TimelineAndroidxPagerFactory(
    private val pageSize: Int = 5,
    private val prefetchDistance: Int = 5,
) {
    private val api = TimelineApi(Server())

    fun create(): Pager<GetFeedRequest, Post> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, prefetchDistance = prefetchDistance),
            pagingSourceFactory = { TimelineAndroidxPagingSource(api, pageSize) }
        )
    }
}