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
import org.mobilenativefoundation.storex.paging.*
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.internal.api.FetchingStateHolder


@Suppress("UNCHECKED_CAST")
@OptIn(InternalSerializationApi::class)
class SelfUpdatingItemPresenter<Id : Comparable<Id>, K : Any, V : Identifiable<Id>, E : Any>(
    private val itemFetcher: Fetcher<Id, V>?,
    private val registry: KClassRegistry<Id, K, V, E>,
    private val errorFactory: ErrorFactory<E>,
    private val itemCache: ItemCache<Id, V>,
    private val db: PagingDb?,
    private val fetchingStateHolder: FetchingStateHolder<Id, K>
) {

    fun present(id: Quantifiable<Id>): SelfUpdatingItem<Id, V, E> {
        val presenter: @Composable (Flow<SelfUpdatingItem.Event<Id, V, E>>) -> ItemState<Id, V, E> = { events ->
            itemState(id, events)
        }
        return SelfUpdatingItem(presenter)
    }

    @Composable
    private fun itemState(
        id: Quantifiable<Id>,
        events: Flow<SelfUpdatingItem.Event<Id, V, E>>
    ): ItemState<Id, V, E> {
        val encodedId = remember(id) { Json.encodeToString(registry.id.serializer(), id.value) }
        val item by remember { derivedStateOf { itemCache.getItem(id) } }

        var itemVersion by remember(id) { mutableStateOf(0L) }
        var singleLoadState by remember(id) {
            mutableStateOf<SingleLoadState<E>>(
                if (item != null) {
                    itemVersion++
                    SingleLoadState.Loaded
                } else {
                    SingleLoadState.Initial
                }
            )
        }

        val state by remember(item, singleLoadState, itemVersion) {
            derivedStateOf { ItemState(item, singleLoadState, itemVersion) }
        }

        LaunchedEffect(id) {
            fetchingStateHolder.updateMaxItemAccessedSoFar(id)
        }

        LaunchedEffect(id, events) {
            events.collect { event ->
                when (event) {
                    is SelfUpdatingItem.Event.Clear -> {
                        itemCache.removeItem(id)
                        itemVersion = 0L
                        singleLoadState = SingleLoadState.Cleared
                    }

                    is SelfUpdatingItem.Event.Refresh -> {
                        singleLoadState = SingleLoadState.Refreshing
                        if (itemFetcher == null) {
                            throw IllegalStateException("Item fetcher is null")
                        }
                        when (val fetcherResult = itemFetcher.invoke(id.value).first()) {
                            is FetcherResult.Data -> {
                                val item = fetcherResult.value
                                itemCache.updateItem(item)
                                singleLoadState = SingleLoadState.Loaded
                                itemVersion++
                            }

                            is FetcherResult.Error.Custom<*> -> {
                                val error = fetcherResult.error as E
                                singleLoadState = SingleLoadState.Error.Refreshing(error)
                            }

                            is FetcherResult.Error.Exception -> {
                                val error = handleFetcherException(fetcherResult)
                                singleLoadState = SingleLoadState.Error.Refreshing(error)
                            }

                            is FetcherResult.Error.Message -> {
                                val error = errorFactory.create(fetcherResult.message)
                                singleLoadState = SingleLoadState.Error.Refreshing(error)
                            }
                        }
                    }

                    is SelfUpdatingItem.Event.Update -> {
                        itemCache.updateItem(event.value)
                        singleLoadState = SingleLoadState.Loaded
                        itemVersion++
                    }
                }
            }
        }

        LaunchedEffect(id) {
            db?.itemQueries?.getItem(encodedId)?.asFlow()?.map { it.executeAsOneOrNull() }?.collect {
                if (item == null) {
                    if (itemCache.getItem(id) != null) {
                        itemCache.removeItem(id)
                        itemVersion = 0L
                        singleLoadState = SingleLoadState.Cleared
                    }
                } else if (item != itemCache.getItem(id)) {
                    val itemValue = item as V
                    itemCache.updateItem(itemValue)
                    singleLoadState = SingleLoadState.Loaded
                    itemVersion++
                }
            }
        }

        return state
    }

    private fun handleFetcherException(fetcherResult: FetcherResult.Error.Exception): E {
        return if (fetcherResult.error is PagingError) {
            val pagingError = fetcherResult.error as PagingError
            val error = Json.decodeFromString(registry.error.serializer(), pagingError.encodedError)
            error
        } else {
            errorFactory.create(fetcherResult.error)
        }
    }
}