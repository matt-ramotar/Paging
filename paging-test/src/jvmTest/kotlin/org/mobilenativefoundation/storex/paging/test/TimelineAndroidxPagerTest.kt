package org.mobilenativefoundation.storex.paging.test

import androidx.paging.testing.asSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.storex.paging.LoadDirection
import org.mobilenativefoundation.storex.paging.LoadStrategy
import org.mobilenativefoundation.storex.paging.PagingSource
import org.mobilenativefoundation.storex.paging.test.utils.TimelineAndroidxPagerFactory
import org.mobilenativefoundation.storex.paging.test.utils.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.PostId
import kotlin.test.Test


@OptIn(ExperimentalCoroutinesApi::class)
class TimelineAndroidxPagerTest {
    private val coroutineDispatcher = StandardTestDispatcher()

    private val testScope = TestScope(coroutineDispatcher)

    private fun loadParams(cursor: String, pageSize: Int) = PagingSource.LoadParams(
        GetFeedRequest(
            PostId(cursor),
            pageSize,
        ),
        strategy = LoadStrategy.SkipCache,
        direction = LoadDirection.Append
    )

    @Test
    fun pagingState_givenEmptyFlow_shouldEagerLoad() = testScope.runTest {


        val state = TimelineAndroidxPagerFactory().create().flow


        val posts = state.asSnapshot()
        println(posts)

//        state.test {
//            // BECAUSE OF EAGER LOADING WE DON'T COLLECT THE OTHER EMISSIONS, JUST THE LAST WHICH IS ALL LOADED DATA AND NOT LOADING STATUS
//            val eagerLoading = awaitItem()
//            println("ITEM = $eagerLoading")
//            assertIs<PagingLoadState.NotLoading>(eagerLoading.loadStates.append)
//            assertEquals(100, eagerLoading.ids.size)
//
//            expectNoEvents()
//        }
    }

}