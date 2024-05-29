package org.mobilenativefoundation.storex.paging.utils.timeline.server.services

import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedResponse
import org.mobilenativefoundation.storex.paging.utils.timeline.server.db.TimelineDb

class FeedService(
    private val db: TimelineDb
) {
    fun getFeed(key: GetFeedRequest): GetFeedResponse {
        println("GETTING FEED $key")
        val posts = db.postCollection().getAll()
        println("POSTS = $posts")
        val indexOfFirst = posts.indexOfFirst { it.id == key.cursor }
        println("INDEX OF FIRST = $indexOfFirst")
        val indexOfLast = minOf((indexOfFirst + key.size), posts.lastIndex)
        println("INDEX OF LAST = $indexOfLast")
        println("INDEX OF NEXT CURSOR = $indexOfLast")
        val nextCursor = if (indexOfLast < posts.size) posts[indexOfLast].id else null
        println("NEXT CURSOR = $nextCursor")

        return GetFeedResponse(
            posts = posts.subList(indexOfFirst, indexOfLast),
            nextCursor = nextCursor
        )
    }
}
