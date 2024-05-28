package org.mobilenativefoundation.storex.paging.utils.timeline

import kotlinx.coroutines.test.StandardTestDispatcher
import org.mobilenativefoundation.storex.paging.Pager
import org.mobilenativefoundation.storex.paging.PagingConfig
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.Post

class TimelinePagerFactory {

    fun create(): Pager<String, GetFeedRequest, Post, Throwable> {

        val coroutineDispatcher = StandardTestDispatcher()


        return Pager.Builder<String, GetFeedRequest, Post>(
            pagingConfig = PagingConfig(
                placeholderId = "PLACEHOLDER_ID",
                initialKey = GetFeedRequest("1", 50)
            )
        ).coroutineDispatcher(coroutineDispatcher)
            .build()
    }
}