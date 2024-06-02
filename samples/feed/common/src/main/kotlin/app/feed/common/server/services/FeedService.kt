package app.feed.common.server.services

import app.feed.common.models.GetFeedRequest
import app.feed.common.models.GetFeedResponse
import app.feed.common.server.db.TimelineDb

class FeedService(
    private val db: TimelineDb
) {
    fun getFeed(key: GetFeedRequest): GetFeedResponse {


        println("GETTING FEED $key")
        val posts = db.postCollection().getAll().reversed()

        println("POSTS = $posts")
        val indexOfFirst = if (key.cursor == null) 0 else posts.indexOfFirst { it.id == key.cursor }
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
