package org.mobilenativefoundation.storex.paging.test.utils.models

import kotlinx.serialization.Serializable

@Serializable
data class GetFeedRequest(
    val cursor: PostId,
    val size: Int,
    val headers: MutableMap<String, String> = mutableMapOf()
) : Comparable<GetFeedRequest> {
    override fun compareTo(other: GetFeedRequest): Int {
        return cursor - other.cursor
    }
}