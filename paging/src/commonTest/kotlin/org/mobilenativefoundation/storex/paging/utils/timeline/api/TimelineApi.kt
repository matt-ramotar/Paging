package org.mobilenativefoundation.storex.paging.utils.timeline.api

import kotlinx.coroutines.delay
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedResponse
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetPostRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetPostResponse
import org.mobilenativefoundation.storex.paging.utils.timeline.server.Server

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