package app.feed.common.api

import app.feed.common.models.GetFeedRequest
import app.feed.common.models.GetFeedResponse
import app.feed.common.models.GetPostRequest
import app.feed.common.models.GetPostResponse
import app.feed.common.server.Server
import kotlinx.coroutines.delay

class TimelineApi(private val server: Server) {
    suspend fun getPost(request: GetPostRequest): GetPostResponse {
        delay(100)
        return server.getPost(request)
    }

    suspend fun getFeed(request: GetFeedRequest): GetFeedResponse {
        delay(100)
        return server.getFeed(request)
    }
}