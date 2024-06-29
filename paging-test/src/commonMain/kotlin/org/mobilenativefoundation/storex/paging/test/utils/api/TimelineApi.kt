package org.mobilenativefoundation.storex.paging.test.utils.api

import kotlinx.coroutines.delay
import org.mobilenativefoundation.storex.paging.test.utils.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.GetFeedResponse
import org.mobilenativefoundation.storex.paging.test.utils.models.GetPostRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.GetPostResponse
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