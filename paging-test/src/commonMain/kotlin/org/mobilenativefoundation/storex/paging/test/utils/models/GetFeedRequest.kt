package org.mobilenativefoundation.storex.paging.test.utils.models

import kotlinx.serialization.Serializable


@Serializable
data class GetFeedRequest(
    val cursor: PostId?,
    val size: Int,
    val headers: MutableMap<String, String> = mutableMapOf()
) : Comparable<GetFeedRequest> {
    override fun compareTo(other: GetFeedRequest): Int {

        return if (cursor != null && other.cursor!= null) {
            cursor - other.cursor
        } else if (cursor != null) {
            1
        } else if (other.cursor != null) {
            -1
        } else {
            0
        }
    }
}