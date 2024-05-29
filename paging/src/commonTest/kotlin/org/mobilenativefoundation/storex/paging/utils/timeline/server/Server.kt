package org.mobilenativefoundation.storex.paging.utils.timeline.server

import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedResponse
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetPostRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetPostResponse
import org.mobilenativefoundation.storex.paging.utils.timeline.server.db.TimelineDb
import org.mobilenativefoundation.storex.paging.utils.timeline.server.services.FeedService
import org.mobilenativefoundation.storex.paging.utils.timeline.server.services.PostService

class Server {

    private val db = TimelineDb()

    private val feedService = FeedService(db)
    private val postService = PostService(db)

    fun getPost(request: GetPostRequest): GetPostResponse {
        return postService.getPost(request)
    }

    fun getFeed(request: GetFeedRequest): GetFeedResponse {
        return feedService.getFeed(request)
    }

}