package app.feed.common.models

import kotlinx.serialization.Serializable
import org.mobilenativefoundation.storex.paging.Identifiable

@Serializable
data class Post(
    override val id: PostId,
    val title: String,
) : Identifiable<String>
