package org.mobilenativefoundation.storex.paging.test.utils.server.db

import org.mobilenativefoundation.storex.paging.test.utils.server.db.PostCollection

class TimelineDb {

    private val postCollection = PostCollection()
    fun postCollection(): PostCollection = postCollection
}