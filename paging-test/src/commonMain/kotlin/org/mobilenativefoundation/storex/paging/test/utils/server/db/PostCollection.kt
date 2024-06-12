package org.mobilenativefoundation.storex.paging.test.utils.server.db

import org.mobilenativefoundation.storex.paging.test.utils.models.Post
import org.mobilenativefoundation.storex.paging.test.utils.models.PostId

class PostCollection {
    private val allIds: MutableList<PostId> = mutableListOf()
    private val byId: MutableMap<PostId, Post> = mutableMapOf()

    init {
        List(500) { index ->
            val id = index.toString()
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