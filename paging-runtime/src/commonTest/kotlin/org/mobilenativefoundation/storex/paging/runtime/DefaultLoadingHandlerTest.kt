package org.mobilenativefoundation.storex.paging.runtime

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.mobilenativefoundation.storex.paging.custom.Middleware
import org.mobilenativefoundation.storex.paging.runtime.internal.logger.impl.TestPagingLogger
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ExponentialBackoff
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.LoadParamsQueue
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.OperationApplier
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.PagingStateManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.QueueManager
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.RetryBookkeeper
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.DefaultLoadingHandler
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.NormalizedStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.PageLoadState
import org.mobilenativefoundation.storex.paging.testUtils.CursorIdentifier
import org.mobilenativefoundation.storex.paging.testUtils.Post
import org.mobilenativefoundation.storex.paging.testUtils.TimelineRequest
import kotlin.test.BeforeTest
import kotlin.test.Test

class DefaultLoadingHandlerTest {

    private val appendQueue = mock<LoadParamsQueue<TimelineRequest>>()
    private val prependQueue = mock<LoadParamsQueue<TimelineRequest>>()

    private val pagingState = MutableStateFlow(PagingState.initial<CursorIdentifier>())
    private val fetchingState = MutableStateFlow(FetchingState<CursorIdentifier, TimelineRequest>())

    private val pagingStateManager = mock<PagingStateManager<CursorIdentifier>> {
        every { pagingState } returns this@DefaultLoadingHandlerTest.pagingState
    }
    private val store = mock<NormalizedStore<CursorIdentifier, TimelineRequest, Post>>()
    private val queueManager = mock<QueueManager<TimelineRequest>> {
        every { appendQueue } returns this@DefaultLoadingHandlerTest.appendQueue
        every { prependQueue } returns this@DefaultLoadingHandlerTest.prependQueue
    }
    private val fetchingStateHolder = mock<FetchingStateHolder<CursorIdentifier, TimelineRequest>> {
        every { state } returns this@DefaultLoadingHandlerTest.fetchingState
    }
    private val errorHandlingStrategy = ErrorHandlingStrategy.Ignore
    private val middleware = emptyList<Middleware<TimelineRequest>>()
    private val operationApplier = mock<OperationApplier<CursorIdentifier, TimelineRequest, Post>>()
    private val retryBookkeeper = mock<RetryBookkeeper<CursorIdentifier, TimelineRequest>>()
    private val logger = TestPagingLogger()
    private val exponentialBackoff = mock<ExponentialBackoff>()

    private lateinit var loadingHandler: DefaultLoadingHandler<CursorIdentifier, TimelineRequest, Post>

    @BeforeTest
    fun setup() {
        loadingHandler = DefaultLoadingHandler(
            store,
            pagingStateManager,
            queueManager,
            fetchingStateHolder,
            errorHandlingStrategy,
            middleware,
            operationApplier,
            retryBookkeeper,
            logger,
            exponentialBackoff
        )
    }

    private fun createFakePost(id: Int): Post {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return Post(
            id = CursorIdentifier("$timestamp-$id"),
            title = "title-$id",
            body = "body-$id",
            authorId = "author-$id"
        )
    }

    @Test
    fun handleAppendLoading_whenLoadIsSuccess_thenShouldUpdateStatesAndEnqueueNext() = runTest {
        // Given
        val cursor = "1"
        val nextCursor = "21"
        val size = 20
        val key = TimelineRequest(cursor, size)
        val strategy = LoadStrategy.SkipCache
        val direction = LoadDirection.Append
        val nextKey = TimelineRequest(nextCursor, size)
        val loadParams = PagingSource.LoadParams(key, strategy, direction)

        val expectedPosts = List(size) { createFakePost(it + 1) }

        val expectedStoreFlow = flowOf(
            PageLoadState.Success(
                isTerminal = false,
                snapshot = ItemSnapshotList(expectedPosts),
                prevKey = null,
                nextKey = nextKey,
                source = PageLoadState.Success.Source.Network
            )
        )

        everySuspend { store.loadPage(eq(loadParams)) }.returns(expectedStoreFlow)
        everySuspend { operationApplier.applyOperations(any(), any(), any(), any()) } returns ItemSnapshotList(expectedPosts)

        // When
        loadingHandler.handleAppendLoading(loadParams, true)

        // Then
        verifySuspend(exactly(1)) { store.loadPage(eq(loadParams)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMaxRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMinRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { pagingStateManager.updateWithAppendLoading() }
        verifySuspend(exactly(1)) { pagingStateManager.updateWithAppendData(eq(expectedPosts.map { it.id }), eq(false)) }
        verifySuspend(exactly(1)) {
            queueManager.enqueueAppend(
                eq(
                    Action.Enqueue(
                        key = nextKey,
                        direction = direction,
                        strategy = strategy,
                        jump = false
                    )
                )
            )
        }
    }

    @Test
    fun handlePrependLoading_whenLoadIsSuccess_thenShouldUpdateStatesAndEnqueuePrev() = runTest {
        // Given
        val cursor = "21"
        val prevCursor = "1"
        val size = 20
        val key = TimelineRequest(cursor, size)
        val strategy = LoadStrategy.SkipCache
        val direction = LoadDirection.Prepend
        val prevKey = TimelineRequest(prevCursor, size)
        val loadParams = PagingSource.LoadParams(key, strategy, direction)

        val expectedPosts = List(size) { createFakePost(it + 21) }

        val expectedStoreFlow = flowOf(
            PageLoadState.Success(
                isTerminal = false,
                snapshot = ItemSnapshotList(expectedPosts),
                prevKey = prevKey,
                nextKey = null,
                source = PageLoadState.Success.Source.Network
            )
        )

        everySuspend { store.loadPage(eq(loadParams)) }.returns(expectedStoreFlow)
        everySuspend { operationApplier.applyOperations(any(), any(), any(), any()) } returns ItemSnapshotList(expectedPosts)

        // When
        loadingHandler.handlePrependLoading(loadParams)

        // Then
        verifySuspend(exactly(1)) { store.loadPage(eq(loadParams)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMaxRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMinRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { pagingStateManager.updateWithPrependLoading() }
        verifySuspend(exactly(1)) { pagingStateManager.updateWithPrependData(eq(expectedPosts.map { it.id }), eq(false)) }
        verifySuspend(exactly(1)) {
            queueManager.enqueuePrepend(
                eq(
                    Action.Enqueue(
                        key = prevKey,
                        direction = direction,
                        strategy = strategy,
                        jump = false
                    )
                )
            )
        }
    }

    @Test
    fun handleAppendLoading_givenPassThroughErrorStrategy_whenError_thenShouldUpdateStates() = runTest {
        // Given
        val cursor = "1"
        val size = 20
        val key = TimelineRequest(cursor, size)
        val strategy = LoadStrategy.SkipCache
        val direction = LoadDirection.Append
        val loadParams = PagingSource.LoadParams(key, strategy, direction)
        val expectedPosts = List(size) { createFakePost(it + 1) }
        val errorHandlingStrategy = ErrorHandlingStrategy.PassThrough

        loadingHandler = DefaultLoadingHandler(
            store,
            pagingStateManager,
            queueManager,
            fetchingStateHolder,
            errorHandlingStrategy,
            middleware,
            operationApplier,
            retryBookkeeper,
            logger,
            exponentialBackoff
        )

        val expectedStoreFlow = flowOf(
            PageLoadState.Error.Message<CursorIdentifier, TimelineRequest, Post>("", true)
        )

        everySuspend { store.loadPage(eq(loadParams)) }.returns(expectedStoreFlow)
        everySuspend { operationApplier.applyOperations(any(), any(), any(), any()) } returns ItemSnapshotList(expectedPosts)

        // When
        loadingHandler.handleAppendLoading(loadParams)

        // Then
        verifySuspend(exactly(1)) { store.loadPage(eq(loadParams)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMaxRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMinRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { store.clearPage(eq(loadParams.key)) }
        verifySuspend(exactly(1)) { pagingStateManager.updateWithAppendError(any()) }
        verifySuspend(exactly(1)) { queueManager.updateExistingPendingJob(eq(loadParams.key), eq(false), eq(true)) }
    }

    @Test
    fun handlePrependLoading_givenPassThroughErrorStrategy_whenError_thenShouldUpdateStates() = runTest {
        // Given
        val cursor = "21"
        val size = 20
        val key = TimelineRequest(cursor, size)
        val strategy = LoadStrategy.SkipCache
        val direction = LoadDirection.Prepend
        val loadParams = PagingSource.LoadParams(key, strategy, direction)
        val expectedPosts = List(size) { createFakePost(it + 21) }
        val errorHandlingStrategy = ErrorHandlingStrategy.PassThrough

        loadingHandler = DefaultLoadingHandler(
            store,
            pagingStateManager,
            queueManager,
            fetchingStateHolder,
            errorHandlingStrategy,
            middleware,
            operationApplier,
            retryBookkeeper,
            logger,
            exponentialBackoff
        )

        val expectedStoreFlow = flowOf(
            PageLoadState.Error.Message<CursorIdentifier, TimelineRequest, Post>("", true)
        )

        everySuspend { store.loadPage(eq(loadParams)) }.returns(expectedStoreFlow)
        everySuspend { operationApplier.applyOperations(any(), any(), any(), any()) } returns ItemSnapshotList(expectedPosts)

        // When
        loadingHandler.handlePrependLoading(loadParams)

        // Then
        verifySuspend(exactly(1)) { store.loadPage(eq(loadParams)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMaxRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMinRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { store.clearPage(eq(loadParams.key)) }
        verifySuspend(exactly(1)) { pagingStateManager.updateWithPrependError(any()) }
        verifySuspend(exactly(1)) { queueManager.updateExistingPendingJob(eq(loadParams.key), eq(false), eq(true)) }
    }

    @Test
    fun handleAppendLoading_givenIgnoreErrorStrategy_whenError_thenShouldNotUpdatePagingState() = runTest {
        // Given
        val cursor = "1"
        val size = 20
        val key = TimelineRequest(cursor, size)
        val strategy = LoadStrategy.SkipCache
        val direction = LoadDirection.Append
        val loadParams = PagingSource.LoadParams(key, strategy, direction)
        val expectedPosts = List(size) { createFakePost(it + 1) }
        val errorHandlingStrategy = ErrorHandlingStrategy.Ignore

        loadingHandler = DefaultLoadingHandler(
            store,
            pagingStateManager,
            queueManager,
            fetchingStateHolder,
            errorHandlingStrategy,
            middleware,
            operationApplier,
            retryBookkeeper,
            logger,
            exponentialBackoff
        )

        val expectedStoreFlow = flowOf(
            PageLoadState.Error.Message<CursorIdentifier, TimelineRequest, Post>("", true)
        )

        everySuspend { store.loadPage(eq(loadParams)) }.returns(expectedStoreFlow)
        everySuspend { operationApplier.applyOperations(any(), any(), any(), any()) } returns ItemSnapshotList(expectedPosts)

        // When
        loadingHandler.handleAppendLoading(loadParams)

        // Then
        verifySuspend(exactly(1)) { store.loadPage(eq(loadParams)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMaxRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMinRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { store.clearPage(eq(loadParams.key)) }
        verifySuspend(exactly(0)) { pagingStateManager.updateWithAppendError(any()) }
        verifySuspend(exactly(1)) { queueManager.updateExistingPendingJob(eq(loadParams.key), eq(false), eq(true)) }
    }

    @Test
    fun handlePrependLoading_givenIgnoreErrorStrategy_whenError_thenShouldNotUpdatePagingState() = runTest {
        // Given
        val cursor = "21"
        val size = 20
        val key = TimelineRequest(cursor, size)
        val strategy = LoadStrategy.SkipCache
        val direction = LoadDirection.Prepend
        val loadParams = PagingSource.LoadParams(key, strategy, direction)
        val expectedPosts = List(size) { createFakePost(it + 21) }
        val errorHandlingStrategy = ErrorHandlingStrategy.Ignore

        loadingHandler = DefaultLoadingHandler(
            store,
            pagingStateManager,
            queueManager,
            fetchingStateHolder,
            errorHandlingStrategy,
            middleware,
            operationApplier,
            retryBookkeeper,
            logger,
            exponentialBackoff
        )

        val expectedStoreFlow = flowOf(
            PageLoadState.Error.Message<CursorIdentifier, TimelineRequest, Post>("", true)
        )

        everySuspend { store.loadPage(eq(loadParams)) }.returns(expectedStoreFlow)
        everySuspend { operationApplier.applyOperations(any(), any(), any(), any()) } returns ItemSnapshotList(expectedPosts)

        // When
        loadingHandler.handlePrependLoading(loadParams)

        // Then
        verifySuspend(exactly(1)) { store.loadPage(eq(loadParams)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMaxRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { fetchingStateHolder.updateMinRequestSoFar(eq(key)) }
        verifySuspend(exactly(1)) { store.clearPage(eq(loadParams.key)) }
        verifySuspend(exactly(0)) { pagingStateManager.updateWithPrependError(any()) }
        verifySuspend(exactly(1)) { queueManager.updateExistingPendingJob(eq(loadParams.key), eq(false), eq(true)) }
    }
}