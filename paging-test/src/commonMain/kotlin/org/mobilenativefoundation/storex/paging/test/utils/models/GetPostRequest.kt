package org.mobilenativefoundation.storex.paging.test.utils.models

import kotlinx.serialization.Serializable

@Serializable
data class GetPostRequest(
    val id: PostId,
    val headers: MutableMap<String, String> = mutableMapOf()
)