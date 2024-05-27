package org.mobilenativefoundation.storex.paging.utils.timeline.server.services

import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetPostRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetPostResponse
import org.mobilenativefoundation.storex.paging.utils.timeline.server.db.TimelineDb


class PostService(
    private val db: TimelineDb
) {
    fun getPost(request: GetPostRequest): GetPostResponse =
        GetPostResponse(db.postCollection().getById(request.id))
}

