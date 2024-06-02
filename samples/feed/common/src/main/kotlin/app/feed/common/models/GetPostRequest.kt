package app.feed.common.models

import kotlinx.serialization.Serializable

@Serializable
data class GetPostRequest(
    val id: PostId,
    val headers: MutableMap<String, String> = mutableMapOf()
)