package org.mobilenativefoundation.storex.paging.utils.timeline.models

import org.mobilenativefoundation.store5.core.Identifiable

data class Post(
    override val id: String
) : Identifiable<String>
