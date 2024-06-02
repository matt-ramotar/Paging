package app.feed.common.server.db

import app.feed.common.models.Post
import app.feed.common.models.PostId
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes


class PostCollection {
    private val allIds: MutableList<PostId> = mutableListOf()
    private val byId: MutableMap<PostId, Post> = mutableMapOf()

    val userIds = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    private var lastTimestamp = Clock.System.now()

    fun getTimestamp(): Long {
        val timestamp = lastTimestamp.plus(1.minutes)
        lastTimestamp = timestamp
        return timestamp.toEpochMilliseconds()
    }


    init {
        List(500) { index ->
            val id = (index + 1).toString()
            val postId = PostId(id)
            allIds.add(postId)
            val userId = userIds[index.mod(10)]
            byId[postId] = Post(
                id = postId,
                userId = userId.toString(),
                text = "${id}. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus hendrerit sed lacus commodo interdum.",
                createdAt = getTimestamp(),
                retweetCount = 0,
                favoriteCount = 0,
                isRetweetedByViewer = false,
                isLikedByViewer = false,
                parentPostId = null
            )
        }
    }

    fun getAll(): List<Post> {
        return allIds.mapNotNull { byId[it] }
    }

    fun getById(id: PostId): Post? {
        return byId[id]
    }

    fun addAll(index: Int, posts: List<Post>) {
        allIds.addAll(index, posts.map { it.id })
        byId.putAll(posts.associateBy { it.id })
    }
}