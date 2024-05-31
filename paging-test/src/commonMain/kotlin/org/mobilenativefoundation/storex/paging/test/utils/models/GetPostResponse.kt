package org.mobilenativefoundation.storex.paging.test.utils.models

import kotlinx.serialization.Serializable

@Serializable
data class GetPostResponse(
    val post: Post?
)