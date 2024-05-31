package org.mobilenativefoundation.storex.paging.test

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.test.utils.TimelineAndroidxPagingSource
import org.mobilenativefoundation.storex.paging.test.utils.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.PostId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs


@OptIn(ExperimentalCoroutinesApi::class)
class RealPagerTest {
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

        val pager =
            org.mobilenativefoundation.storex.paging.test.utils.TimelinePagerFactory().create(coroutineDispatcher)

        advanceUntilIdle()

        val state = pager.launchPagingFlow()

        state.test {
            // BECAUSE OF EAGER LOADING WE DON'T COLLECT THE OTHER EMISSIONS, JUST THE LAST WHICH IS ALL LOADED DATA AND NOT LOADING STATUS
            val eagerLoading = awaitItem()
            println("ITEM = $eagerLoading")
            assertIs<PagingLoadState.NotLoading>(eagerLoading.loadStates.append)
            assertEquals(100, eagerLoading.ids.size)

            expectNoEvents()
        }
    }

    @Test
    fun pagingState_givenEmptyFlow_whenAndroidxPagingSource_shouldEagerLoad() = testScope.runTest {

        val pageSize = 20
        val prefetchDistance = 100

        val pager =
            org.mobilenativefoundation.storex.paging.test.utils.TimelinePagerFactory(
                pageSize = pageSize,
                prefetchDistance = prefetchDistance
            )
                .create(coroutineDispatcher, androidxPagingSourceFactory = { api ->
                    TimelineAndroidxPagingSource(api, pageSize = pageSize)
                })

        advanceUntilIdle()

        val state = pager.launchPagingFlow()

        state.test {
            // BECAUSE OF EAGER LOADING WE DON'T COLLECT THE OTHER EMISSIONS, JUST THE LAST WHICH IS ALL LOADED DATA AND NOT LOADING STATUS
            val eagerLoading = awaitItem()
            println("ITEM = $eagerLoading")
            assertIs<PagingLoadState.NotLoading>(eagerLoading.loadStates.append)
            assertEquals(100, eagerLoading.ids.size)

            expectNoEvents()
        }
    }

    @Test
    fun pagingState_givenCompletedEagerLoad_whenProcessQueueRequest_shouldNotBypassFetchingStrategy() =
        testScope.runTest {

            val pageSize = 20
            val prefetchDistance = 100

            val pager = org.mobilenativefoundation.storex.paging.test.utils.TimelinePagerFactory(
                pageSize,
                prefetchDistance
            ).create(coroutineDispatcher)

            advanceUntilIdle()

            val state = pager.launchPagingFlow {
                PagingRequest.processQueue(LoadDirection.Append)
            }

            state.test {
                val eagerLoading = awaitItem()
                assertIs<PagingLoadState.NotLoading>(eagerLoading.loadStates.append)
                assertEquals(100, eagerLoading.ids.size)

                // Doesn't fetch further because of prefetchDistance

                expectNoEvents()
            }


        }

    @Test
    fun pagingState_givenCompletedEagerLoad_whenSkipQueueRequest_shouldBypassFetchingStrategy() =
        testScope.runTest {

            val pageSize = 20
            val prefetchDistance = 100

            fun nextCursor(prefetchDistance: Int, request: Int) =
                (prefetchDistance + 1 + (pageSize * (request - 1))).toString()

            fun nextKey(prefetchDistance: Int, request: Int) =
                GetFeedRequest(PostId(nextCursor(prefetchDistance, request)), pageSize)

            val requests = MutableSharedFlow<PagingRequest<GetFeedRequest>>(replay = 5)

            val pager = org.mobilenativefoundation.storex.paging.test.utils.TimelinePagerFactory(
                pageSize,
                prefetchDistance
            ).create(coroutineDispatcher)

            advanceUntilIdle()

            val state = pager.pagingFlow(requests, RecompositionMode.Immediate)

            state.test {
                val eagerLoading = awaitItem()
                assertIs<PagingLoadState.NotLoading>(eagerLoading.loadStates.append)
                assertEquals(100, eagerLoading.ids.size)


//                pager.lazyLoad(loadParams(nextCursor(prefetchDistance, 2), pageSize))

                advanceUntilIdle()

                requests.emit(
                    PagingRequest.skipQueue(
                        nextKey(prefetchDistance, 1),
                        LoadDirection.Append
                    ),
                )

                advanceUntilIdle()

                // We don't emit processing state

                val loading1 = awaitItem()
                println("loading1 = $loading1")
                assertIs<PagingLoadState.Loading>(loading1.loadStates.append)
                assertEquals(100, loading1.ids.size)
                assertEquals(List(100) { PostId((it + 1).toString()) }, loading1.ids)


                val success1 = awaitItem()
                assertIs<PagingLoadState.NotLoading>(success1.loadStates.append)
                assertEquals(120, success1.ids.size)
                assertEquals(List(120) { PostId((it + 1).toString()) }, success1.ids)
                println("success1 = $success1")


//                val processing2 = awaitItem()
//                println("processing2 = $processing2")
//                val loading2 = awaitItem()
//                println("loading2 = $loading2")
//                val success2 = awaitItem()
//                println("success2 = $success2")


            }


        }


}