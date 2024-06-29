package app.feed.common.server.services

import app.feed.common.models.GetPostRequest
import app.feed.common.models.GetPostResponse
import app.feed.common.server.db.TimelineDb


class PostService(
    private val db: TimelineDb
) {
    fun getPost(request: GetPostRequest): GetPostResponse =
        GetPostResponse(db.postCollection().getById(request.id))
}

