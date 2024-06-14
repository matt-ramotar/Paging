package org.mobilenativefoundation.storex.paging.internal.impl

import androidx.compose.runtime.*
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder


@OptIn(InternalSerializationApi::class)
class SelfUpdatingItemPresenterV2<Id : Comparable<Id>, Q : Quantifiable<Id>, K : Any, V : Identifiable<Id, Q>, E : Any>(
    private val registry: KClassRegistry<Id, Q, K, V, E>,
    private val itemMemoryCache: ItemMemoryCache<Q, V>,
    private val fetchingStateHolder: FetchingStateHolder<Id, Q, K>,
    private val updater: Updater<Q, V, *>?,
    private val linkedHashMap: PagingLinkedHashMap<Id, Q, K, V, E>,
    private val itemFetcher: Fetcher<Id, V>?,
    private val db: PagingDb?
) {

    @Composable
    fun present(
        id: Q,
        events: Flow<SelfUpdatingItem.Event<Id, Q, V, E>>
    ): ItemState<Id, Q, V, E> {
        val encoded = remember(id) { Json.encodeToString(registry.q.serializer(), id) }

        var value by remember(id) {
            mutableStateOf(itemMemoryCache[id])
        }

        var itemVersion by remember(id) { mutableStateOf(0L) }

        var singleLoadState: SingleLoadState<E> by remember(id) {
            val item = value
            // This is the initial state, because we are remembering by id
            mutableStateOf(
                if (item != null) {
                    val nextItemVersion = itemVersion + 1
                    itemVersion = nextItemVersion
                    SingleLoadState.Loaded
                } else {
                    SingleLoadState.Initial
                }
            )
        }

        fun updateItemVersion(reducer: (Long) -> Long) {
            itemVersion = reducer(itemVersion)
        }

        fun updateSingleLoadState(reducer: (SingleLoadState<E>) -> SingleLoadState<E>) {
            singleLoadState = reducer(singleLoadState)
        }

        val state by remember(value, singleLoadState, itemVersion) {
            derivedStateOf {
                ItemState(value, singleLoadState, itemVersion)
            }
        }

        LaunchedEffect(id) {
            updateFetchingState(id)
        }

        LaunchedEffect(id, events) {
            handleEvents(id, events, ::updateSingleLoadState, ::updateItemVersion)
        }

        LaunchedEffect(id, value) {
            handleDatabaseUpdates(
                id = id,
                value = value,
                updateItemVersion = ::updateItemVersion,
                updateSingleLoadState = ::updateSingleLoadState
            )
        }

        return state
    }

    private suspend fun handleEvents(
        id: Q,
        events: Flow<SelfUpdatingItem.Event<Id, Q, V, E>>,
        updateSingleLoadState: ((SingleLoadState<E>) -> SingleLoadState<E>) -> Unit,
        updateItemVersion: ((Long) -> Long) -> Unit,
    ) {
        events.collect { event -> handleEvent(id, event, updateSingleLoadState, updateItemVersion) }
    }

    private suspend fun handleEvent(
        id: Q,
        event: SelfUpdatingItem.Event<Id, Q, V, E>,
        updateSingleLoadState: ((SingleLoadState<E>) -> SingleLoadState<E>) -> Unit,
        updateItemVersion: ((Long) -> Long) -> Unit,
    ) {
        when (event) {
            is SelfUpdatingItem.Event.Clear -> handleClear(id, updateSingleLoadState, updateItemVersion)
            is SelfUpdatingItem.Event.Refresh -> handleRefresh(id, updateSingleLoadState, updateItemVersion)
            is SelfUpdatingItem.Event.Update -> handleUpdate(id, event, updateSingleLoadState, updateItemVersion)
        }
    }

    private fun handleClear(
        id: Q,
        updateSingleLoadState: ((SingleLoadState<E>) -> SingleLoadState<E>) -> Unit,
        updateItemVersion: ((Long) -> Long) -> Unit,
    ) {
        // Remove from memory cache
        // Remove from database
        linkedHashMap.removeItem(id)
        updateItemVersion { 0L }
        updateSingleLoadState { SingleLoadState.Cleared }

        // TODO(): Support updating remote
    }

    private suspend fun handleRefresh(
        id: Q,
        updateSingleLoadState: ((SingleLoadState<E>) -> SingleLoadState<E>) -> Unit,
        updateItemVersion: ((Long) -> Long) -> Unit,
    ) {
        // Load from network

        updateSingleLoadState { SingleLoadState.Refreshing }

        if (itemFetcher == null) {
            throw IllegalStateException("Item fetcher is required.")
        }

        when (val fetcherResult = itemFetcher.invoke(id.value).first()) {
            is FetcherResult.Data -> {
                val item = fetcherResult.value

                // Update memory cache and database
                linkedHashMap.saveItem(item, null)

                // Update state
                updateSingleLoadState { SingleLoadState.Loaded }
                updateItemVersion { it + 1 }
            }

            is FetcherResult.Error.Custom<*> -> TODO()
            is FetcherResult.Error.Exception -> TODO()
            is FetcherResult.Error.Message -> TODO()
        }
    }

    private fun handleUpdate(
        id: Q,
        event: SelfUpdatingItem.Event.Update<Id, Q, V, E>,
        updateSingleLoadState: ((SingleLoadState<E>) -> SingleLoadState<E>) -> Unit,
        updateItemVersion: ((Long) -> Long) -> Unit,
    ) {
        // TODO()
    }

    private fun updateFetchingState(id: Q) {
        fetchingStateHolder.updateMaxItemAccessedSoFar(id)
        fetchingStateHolder.updateMinItemAccessedSoFar(id)
    }

    private suspend fun handleDatabaseUpdates(
        id: Q,
        value: V?,
        updateSingleLoadState: ((SingleLoadState<E>) -> SingleLoadState<E>) -> Unit,
        updateItemVersion: ((Long) -> Long) -> Unit,
    ) {
        db?.let {
            val encoded = Json.encodeToString(registry.q.serializer(), id)
            db.itemQueries.getItem(encoded).asFlow().map { query ->
                val item = query.executeAsOneOrNull()
                if (item != null) {
                    Json.decodeFromString(registry.value.serializer(), item.data_)
                } else {
                    null
                }
            }.collect { item ->
                if (item == null) {
                    if (value != null) {
                        // Remove the item
                        linkedHashMap.removeItem(id)
                        updateItemVersion { 0L }
                    }

                    // TODO(): When else does this happen
                } else if (item != value) {
                    // Re-save to memory cache
                    itemMemoryCache[id] = item

                    updateSingleLoadState {
                        SingleLoadState.Loaded
                    }

                    updateItemVersion { it + 1 }

                    updater?.post(id, item)
                }
            }
        }
    }

}