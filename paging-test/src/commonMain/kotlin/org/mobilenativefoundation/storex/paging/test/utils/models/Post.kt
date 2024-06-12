package org.mobilenativefoundation.storex.paging.test.utils.models

import kotlinx.serialization.Serializable
import org.mobilenativefoundation.storex.paging.Identifiable

@Serializable
data class Post(
    override val id: PostId,
    val userId: String = "",
    val text: String = "",
    val createdAt: Long = Long.MAX_VALUE,
    val retweetCount: Int = 0,
    val favoriteCount: Int = 0,
    val commentCount: Int = 0,
    val isRetweetedByViewer: Boolean = false,
    val isLikedByViewer: Boolean = false,
    val relevanceScore: Float = 0f,
    val trendingScore: Float = 0f,
    val parentPostId: String? = null,
) : Identifiable<String, PostId>
