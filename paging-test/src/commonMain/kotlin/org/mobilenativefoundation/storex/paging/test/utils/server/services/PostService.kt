package org.mobilenativefoundation.storex.paging.test.utils.server.services

import org.mobilenativefoundation.storex.paging.test.utils.models.GetPostRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.GetPostResponse
import org.mobilenativefoundation.storex.paging.test.utils.server.db.TimelineDb


class PostService(
    private val db: TimelineDb
) {
    fun getPost(request: GetPostRequest): GetPostResponse =
        GetPostResponse(db.postCollection().getById(request.id))
}

