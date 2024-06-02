package app.feed.common.server.db

import app.feed.common.models.Post
import app.feed.common.models.PostId

class PostCollection {
    private val allIds: MutableList<PostId> = mutableListOf()
    private val byId: MutableMap<PostId, Post> = mutableMapOf()

    init {
        List(500) { index ->
            val id = (index + 1).toString()
            val postId = PostId(id)
            allIds.add(postId)
            byId[postId] = Post(postId, "${index + 1}")
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