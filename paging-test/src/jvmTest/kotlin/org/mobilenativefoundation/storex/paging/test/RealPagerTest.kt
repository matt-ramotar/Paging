package org.mobilenativefoundation.storex.paging.test


import app.cash.turbine.test
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.ErrorHandlingStrategy
import org.mobilenativefoundation.storex.paging.custom.FetchingStrategy
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.internal.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.internal.api.PagingOperationsManager
import org.mobilenativefoundation.storex.paging.internal.impl.RealFetchingStateHolder
import org.mobilenativefoundation.storex.paging.test.utils.TimelineAndroidxPagingSource
import org.mobilenativefoundation.storex.paging.test.utils.TimelinePagerFactory
import org.mobilenativefoundation.storex.paging.test.utils.api.TimelineApi
import org.mobilenativefoundation.storex.paging.test.utils.models.GetFeedRequest
import org.mobilenativefoundation.storex.paging.test.utils.models.Post
import org.mobilenativefoundation.storex.paging.test.utils.models.PostId
import org.mobilenativefoundation.storex.paging.utils.timeline.server.Server
import kotlin.test.BeforeTest
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

    private lateinit var fetchingStateHolder: FetchingStateHolder<PostId, GetFeedRequest>
    private lateinit var normalizedStore: NormalizedStore<PostId, GetFeedRequest, Post>
    private lateinit var fetchingStrategy: FetchingStrategy<PostId, GetFeedRequest>
    private lateinit var errorHandlingStrategy: ErrorHandlingStrategy
    private lateinit var pagingOperationsManager: PagingOperationsManager<PostId, GetFeedRequest, Post>

    private val server = Server()
    private val api = TimelineApi(server)
    private val pageSize = 20
    private val pagingSource = TimelineAndroidxPagingSource(api, pageSize)

    @BeforeTest
    fun setup() {
        fetchingStateHolder = mockk()
        normalizedStore = mockk()
        fetchingStrategy = mockk()
        errorHandlingStrategy = mockk()
        pagingOperationsManager = mockk()
    }


    @Test
    fun pagingState_givenEmptyFlow_shouldEagerLoad() = testScope.runTest {

        val pager =
            TimelinePagerFactory().create(coroutineDispatcher)

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
    fun pagingFlow_givenCompletedEagerLoad_whenUpdateFetchingState2XPageSize_shouldFetch2X() = testScope.runTest {
        val pageSize = 20
        val prefetchDistance = 100

        fun nextCursor(prefetchDistance: Int, request: Int) =
            (500 - (prefetchDistance + (pageSize * (request - 1)))).toString()

        fun nextKey(prefetchDistance: Int, request: Int) =
            GetFeedRequest(PostId(nextCursor(prefetchDistance, request)), pageSize)

        val requests = MutableSharedFlow<PagingRequest<GetFeedRequest>>(replay = 5)

        val fetchingStateHolder = RealFetchingStateHolder<PostId, GetFeedRequest>()

        val pager = mockPager(coroutineDispatcher, fetchingStateHolder)

        val state = pager.pagingFlow(
            flowOf(PagingRequest.processQueue(LoadDirection.Append)),
            recompositionMode = RecompositionMode.Immediate
        )

        advanceUntilIdle()

//        fetchingStateHolder.updateMinItemAccessedSoFar(PostId("480"))

        state.test {

            val eagerLoading = awaitItem()
            assertIs<PagingLoadState.NotLoading>(eagerLoading.loadStates.append)
            assertEquals(100, eagerLoading.ids.size)
            assertEquals((499 downTo 400).map { PostId(it.toString()) }, eagerLoading.ids)

            // Doesn't fetch further because of prefetchDistance
            expectNoEvents()

            advanceUntilIdle()

            launch {
                println("LAUNCHING!!LILY")
                fetchingStateHolder.updateMinItemAccessedSoFar(PostId("480"))
            }

            val loadingPrefetch1 = awaitItem()
            assertIs<PagingLoadState.Loading>(loadingPrefetch1.loadStates.append)
            assertEquals((499 downTo 400).map { PostId(it.toString()) }, loadingPrefetch1.ids)

            val loadedPrefetch1 = awaitItem()
            assertIs<PagingLoadState.NotLoading>(loadedPrefetch1.loadStates.append)
            assertEquals((499 downTo 380).map { PostId(it.toString()) }, loadedPrefetch1.ids)

            advanceUntilIdle()


            launch {
                println("LAUNCHING!!LILY")
                fetchingStateHolder.updateMinItemAccessedSoFar(PostId("460"))

            }
            println("LOADING PREFETCH 1 = ${loadedPrefetch1.ids}")

            val loadingPrefetch2 = awaitItem()
            assertIs<PagingLoadState.Loading>(loadingPrefetch2.loadStates.append)
            assertEquals((499 downTo 380).map { PostId(it.toString()) }, loadingPrefetch2.ids)


            val loadedPrefetch2 = awaitItem()
            assertIs<PagingLoadState.NotLoading>(loadedPrefetch2.loadStates.append)
            assertEquals((499 downTo 360).map { PostId(it.toString()) }, loadedPrefetch2.ids)

            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun pagingFlow_givenCompletedEagerLoad_whenUpdateFetchingState1XPageSize_shouldFetch1X() = testScope.runTest {
        val pageSize = 20
        val prefetchDistance = 100

        fun nextCursor(prefetchDistance: Int, request: Int) =
            (500 - (prefetchDistance + (pageSize * (request - 1)))).toString()

        fun nextKey(prefetchDistance: Int, request: Int) =
            GetFeedRequest(PostId(nextCursor(prefetchDistance, request)), pageSize)

        val requests = MutableSharedFlow<PagingRequest<GetFeedRequest>>(replay = 5)

        val fetchingStateHolder = RealFetchingStateHolder<PostId, GetFeedRequest>()

        val pager = mockPager(coroutineDispatcher, fetchingStateHolder)


        val state = pager.pagingFlow(
            flowOf(PagingRequest.processQueue(LoadDirection.Append)),
            recompositionMode = RecompositionMode.Immediate
        )

        advanceUntilIdle()

        fetchingStateHolder.updateMinItemAccessedSoFar(PostId("480"))

        state.test {

            val eagerLoading = awaitItem()
            assertIs<PagingLoadState.NotLoading>(eagerLoading.loadStates.append)
            assertEquals(100, eagerLoading.ids.size)
            assertEquals((499 downTo 400).map { PostId(it.toString()) }, eagerLoading.ids)

            // Doesn't fetch further because of prefetchDistance
            expectNoEvents()

            advanceUntilIdle()

            val loadingPrefetch1 = awaitItem()
            assertIs<PagingLoadState.Loading>(loadingPrefetch1.loadStates.append)
            assertEquals((499 downTo 400).map { PostId(it.toString()) }, loadingPrefetch1.ids)

            val loadedPrefetch1 = awaitItem()
            assertIs<PagingLoadState.NotLoading>(loadedPrefetch1.loadStates.append)
            assertEquals((499 downTo 380).map { PostId(it.toString()) }, loadedPrefetch1.ids)

            expectNoEvents()
        }
    }


    @Test
    fun pagingState_givenCompletedEagerLoad_whenSkipQueueRequest_shouldBypassFetchingStrategy() =
        testScope.runTest {

            val pageSize = 20
            val prefetchDistance = 100

            fun nextCursor(prefetchDistance: Int, request: Int) =
                (499 - (prefetchDistance + (pageSize * (request - 1)))).toString()

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
                assertEquals((499 downTo 400).toList().map { PostId(it.toString()) }, loading1.ids)


                val success1 = awaitItem()
                assertIs<PagingLoadState.NotLoading>(success1.loadStates.append)
                assertEquals(120, success1.ids.size)
                assertEquals((499 downTo 380).toList().map { PostId(it.toString()) }, success1.ids)
                println("success1 = $success1")


//                val processing2 = awaitItem()
//                println("processing2 = $processing2")
//                val loading2 = awaitItem()
//                println("loading2 = $loading2")
//                val success2 = awaitItem()
//                println("success2 = $success2")


            }


        }

//
//    @Test
//    fun addOperation_givenOperation_shouldAddOperationToOperationsManager() = testScope.runTest {
//        // Given
//        val operation = TopPosts(TimeRange.DAY)
//
//        val pager = mockPager(coroutineDispatcher)
//
//        every { pager.addOperation(any()) } answers {}
//
//        advanceUntilIdle()
//
//        // When
//
//        pager.addOperation(operation)
//
//        // Then
//        verify { pagingOperationsManager.addOperation(operation) }
//    }
//
//    @Test
//    fun removeOperation_givenOperation_shouldRemoveOperationFromOperationsManager() = testScope.runTest {
//        // Given
//        val operation = TopPosts(TimeRange.DAY)
//
//        val pager = mockPager(coroutineDispatcher)
//
//        every { pager.removeOperation(any()) } answers {}
//
//        advanceUntilIdle()
//
//        // When
//        pager.removeOperation(operation)
//
//        // Then
//        verify { pagingOperationsManager.removeOperation(operation) }
//    }
//

    @Test
    fun pagingFlow_givenRequests_shouldReturnFlowOfPagingState() = testScope.runTest {
        // Given
        val requests = emptyFlow<PagingRequest<GetFeedRequest>>()
        val pager = mockPager(coroutineDispatcher)

        // When
        val result = pager.pagingFlow(requests, RecompositionMode.Immediate)

        // Then
        assertIs<Flow<PagingState<PostId>>>(result)
    }

//    @Test
//    fun createSelfUpdatingItem_givenId_shouldReturnSelfUpdatingItem() = testScope.runTest {
//        // Given
//        val id = PostId("1")
//        val pager = mockPager(coroutineDispatcher)
//
//        advanceUntilIdle()
//
//        // When
//        val result = pager.createSelfUpdatingItem(id)
//
//        // Then
//        assertIs<SelfUpdatingItem<String, PostId, Post, TimelineError>>(result)
//    }

    private fun mockPager(
        coroutineDispatcher: CoroutineDispatcher,
        fetchingStateHolder: FetchingStateHolder<PostId, GetFeedRequest>? = null,
    ): Pager<PostId, GetFeedRequest, Post> {

        return Pager.Builder<PostId, GetFeedRequest, Post>(
            pagingConfig = PagingConfig(
                placeholderId = PostId(""),
                pageSize = 20,
                prefetchDistance = 100,
                initialKey = GetFeedRequest(null, size = 20)
            ),
        ).coroutineDispatcher(coroutineDispatcher)
            .androidxPagingSource(pagingSource)
            .operationManager(pagingOperationsManager)
            .apply {
                if (fetchingStateHolder != null) {
                    fetchingStateHolder(fetchingStateHolder)
                }
            }
            .build()
    }
}