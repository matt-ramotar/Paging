package org.mobilenativefoundation.storex.paging.utils.timeline

import kotlinx.coroutines.test.StandardTestDispatcher
import org.mobilenativefoundation.storex.paging.Pager
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.Post
import org.mobilenativefoundation.storex.paging.utils.timeline.models.TimelineError
import org.mobilenativefoundation.storex.paging.utils.timeline.models.TimelineTransformationParams

class TimelinePagerFactory {

    fun create(): Pager<String, GetFeedRequest, Post, TimelineError> {

        val coroutineDispatcher = StandardTestDispatcher()


        return Pager.Builder<String, GetFeedRequest, Post, TimelineError, TimelineTransformationParams>()
            .coroutineDispatcher(coroutineDispatcher)
            .transformationParams(TimelineTransformationParams.RemoveThisSomehow)
            .build()
    }
}