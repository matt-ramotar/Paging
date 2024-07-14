package org.mobilenativefoundation.storex.paging.testUtils

import org.mobilenativefoundation.storex.paging.runtime.Identifiable

data class Post(
    override val id: CursorIdentifier,
    val title: String,
    val body: String,
    val authorId: String
) : Identifiable<CursorIdentifier>
