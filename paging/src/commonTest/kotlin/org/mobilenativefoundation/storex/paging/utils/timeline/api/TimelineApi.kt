package org.mobilenativefoundation.storex.paging.utils.timeline.api

import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedResponse
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetPostRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetPostResponse
import org.mobilenativefoundation.storex.paging.utils.timeline.server.Server

class TimelineApi(private val server: Server) {
    fun getPost(request: GetPostRequest): GetPostResponse {
        return server.getPost(request)
    }

    fun getFeed(request: GetFeedRequest): GetFeedResponse {
        return server.getFeed(request)
    }
}