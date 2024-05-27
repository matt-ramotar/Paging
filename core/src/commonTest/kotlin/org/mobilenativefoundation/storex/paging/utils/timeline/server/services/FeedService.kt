package org.mobilenativefoundation.storex.paging.utils.timeline.server.services

import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedResponse
import org.mobilenativefoundation.storex.paging.utils.timeline.server.db.TimelineDb

class FeedService(
    private val db: TimelineDb
) {
    fun getFeed(key: GetFeedRequest): GetFeedResponse {
        val posts = db.postCollection().getAll()
        val indexOfFirst = posts.indexOfFirst { it.id == key.cursor }
        val indexOfLast = indexOfFirst + key.size
        val indexOfNextCursor = indexOfLast + 1
        val nextCursor = if (indexOfNextCursor < posts.size) posts[indexOfNextCursor].id else null

        return GetFeedResponse(
            posts = posts.subList(indexOfFirst, indexOfLast),
            nextCursor = nextCursor
        )
    }
}
