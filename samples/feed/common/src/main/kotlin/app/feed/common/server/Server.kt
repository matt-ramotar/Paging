package app.feed.common.server

import app.feed.common.models.GetFeedRequest
import app.feed.common.models.GetFeedResponse
import app.feed.common.models.GetPostRequest
import app.feed.common.models.GetPostResponse
import app.feed.common.server.db.TimelineDb
import app.feed.common.server.services.FeedService
import app.feed.common.server.services.PostService

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