package app.feed.common.server.db

class TimelineDb {

    private val postCollection = PostCollection()
    fun postCollection(): PostCollection = postCollection
}