package org.mobilenativefoundation.storex.paging.utils.timeline.server.db

import org.mobilenativefoundation.storex.paging.utils.timeline.models.Post
import org.mobilenativefoundation.storex.paging.utils.timeline.models.PostId

class PostCollection {
    private val allIds: MutableList<PostId> = mutableListOf()
    private val byId: MutableMap<PostId, Post> = mutableMapOf()

    init {
        List(500) { index ->
            val id = (index + 1).toString()
            val postId = PostId(id)
            allIds.add(postId)
            byId[postId] = Post(postId)
        }
    }

    fun getAll(): List<Post> {
        return allIds.mapNotNull { byId[it] }
    }

    fun getById(id: PostId): Post? {
        return byId[id]
    }
}