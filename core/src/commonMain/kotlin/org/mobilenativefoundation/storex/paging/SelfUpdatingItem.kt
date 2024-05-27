@file:Suppress("UNCHECKED_CAST")

package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store5.core.Identifiable
import org.mobilenativefoundation.storex.paging.custom.ErrorFactory
import org.mobilenativefoundation.storex.paging.internal.api.DispatcherProvider

// TODO: Design decision to not have each item update pass down UI tree to optimize recompositions only recompose the item
// So each item needs its own presenter
class SelfUpdatingItem<Id : Comparable<Id>, V : Identifiable<Id>, E : Any>(
    private val initialState: ItemState<Id, V, E>,
    private val store: Store<Id, V>,
    private val errorFactory: ErrorFactory<E>
) {
    @Composable
    operator fun invoke(id: Id): ItemState<Id, V, E> {
        var itemState by remember { mutableStateOf(initialState) }

        LaunchedEffect(id) {
            withContext(DispatcherProvider.io) {

                val storeReadRequest = StoreReadRequest.cached(id, refresh = false)
                store.stream(storeReadRequest).distinctUntilChanged().collect { storeReadResponse ->
                    when (storeReadResponse) {
                        is StoreReadResponse.Data -> {
                            val data = storeReadResponse.requireData()
                            itemState = ItemState(
                                item = data,
                                loadState = ItemLoadState.Loaded
                            )
                        }


                        is StoreReadResponse.Loading -> {
                            itemState = itemState.copy(
                                loadState = ItemLoadState.Loading
                            )
                        }

                        is StoreReadResponse.NoNewData -> {
                            itemState = itemState.copy(
                                loadState = ItemLoadState.Loaded
                            )
                        }

                        is StoreReadResponse.Error.Exception -> {
                            itemState = itemState.copy(
                                loadState = ItemLoadState.Error(
                                    errorFactory.create(
                                        storeReadResponse.error
                                    )
                                )
                            )
                        }

                        is StoreReadResponse.Error.Message -> {
                            itemState = itemState.copy(
                                loadState = ItemLoadState.Error(
                                    errorFactory.create(
                                        storeReadResponse.message
                                    )
                                )
                            )
                        }

                        is StoreReadResponse.Error.Custom<*> -> {
                            itemState = itemState.copy(
                                loadState = ItemLoadState.Error(
                                    error = storeReadResponse.error as E
                                )
                            )
                        }

                        StoreReadResponse.Initial -> {
                            itemState = itemState.copy(
                                loadState = ItemLoadState.Initial
                            )
                        }
                    }
                }
            }
        }

        return itemState

    }
}

