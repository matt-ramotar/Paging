package org.mobilenativefoundation.storex.paging.utils.timeline.models

import kotlinx.serialization.Serializable
import org.mobilenativefoundation.storex.paging.Identifiable

@Serializable
data class Post(
    override val id: PostId
) : Identifiable<String>
