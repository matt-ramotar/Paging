package app.feed.common.models

import kotlinx.serialization.Serializable

@Serializable
data class GetPostResponse(
    val post: Post?
)