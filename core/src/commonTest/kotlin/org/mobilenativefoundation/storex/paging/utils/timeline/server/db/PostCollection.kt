package org.mobilenativefoundation.storex.paging.utils.timeline.server.db

import org.mobilenativefoundation.storex.paging.utils.timeline.models.Post

class PostCollection {
    private val allIds: MutableList<String> = mutableListOf()
    private val byId: MutableMap<String, Post> = mutableMapOf()
    fun getAll(): List<Post> {
        return allIds.mapNotNull { byId[it] }
    }

    fun getById(id: String): Post? {
        return byId[id]
    }
}