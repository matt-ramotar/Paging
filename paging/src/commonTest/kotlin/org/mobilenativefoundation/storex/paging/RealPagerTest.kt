package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.BroadcastFrameClock
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.storex.paging.utils.timeline.TimelinePagerFactory
import org.mobilenativefoundation.storex.paging.utils.timeline.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.utils.timeline.models.Post
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealPagerTest {
    private val coroutineDispatcher = StandardTestDispatcher()

    private val job = Job()
    private val clock = BroadcastFrameClock()

    private val testScope = TestScope(coroutineDispatcher + job + clock)

    private lateinit var pager: Pager<String, GetFeedRequest, Post, Throwable>

    @BeforeTest
    fun setup() {
        pager = TimelinePagerFactory().create(coroutineDispatcher)
    }

    @Test
    fun test() = testScope.runTest {

        pager.invoke(emptyFlow())

        val state = pager.pagingState

//        val state = launchMolecule(mode = RecompositionMode.Immediate) {
//            pager.pagingState(emptyFlow())
//        }

        state.test {

            advanceUntilIdle()

            val eagerLoading = awaitItem()
            println(eagerLoading)

            advanceUntilIdle()

            expectNoEvents()
        }
    }

}