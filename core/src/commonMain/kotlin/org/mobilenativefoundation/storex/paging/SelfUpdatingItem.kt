@file:Suppress("UNCHECKED_CAST")

package org.mobilenativefoundation.storex.paging

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.mobilenativefoundation.store5.core.Identifiable

// TODO: Design decision to not have each item update pass down UI tree to optimize recompositions only recompose the item
// So each item needs its own presenter
class SelfUpdatingItem<Id : Comparable<Id>, V : Identifiable<Id>, E : Any>(
    private val presenter: @Composable (events: Flow<SelfUpdatingItemEvent<Id, V, E>>) -> ItemState<Id, V, E>
) {
    private val _events = MutableSharedFlow<SelfUpdatingItemEvent<Id, V, E>>()

    @Composable
    operator fun invoke() = presenter(_events)

    suspend fun emit(event: SelfUpdatingItemEvent<Id, V, E>) {
        _events.emit(event)
    }
}

//
//
//class SelfUpdatingItem<Id : Comparable<Id>, V : Identifiable<Id>, E : Any>(
//    private val initialState: ItemState<Id, V, E>,
//    private val store: Store<Id, V>,
//    private val errorFactory: ErrorFactory<E>
//) {
//    @Composable
//    operator fun invoke(id: Id): ItemState<Id, V, E> {
//        var itemState by remember { mutableStateOf(initialState) }
//
//        LaunchedEffect(id) {
//            withContext(DispatcherProvider.io) {
//
//                val storeReadRequest = StoreReadRequest.cached(id, refresh = false)
//                store.stream(storeReadRequest).distinctUntilChanged().collect { storeReadResponse ->
//                    when (storeReadResponse) {
//                        is StoreReadResponse.Data -> {
//                            val data = storeReadResponse.requireData()
//                            itemState = ItemState(
//                                item = data,
//                                loadState = SingleLoadState.Loaded
//                            )
//                        }
//
//
//                        is StoreReadResponse.Loading -> {
//                            itemState = itemState.copy(
//                                loadState = SingleLoadState.Loading
//                            )
//                        }
//
//                        is StoreReadResponse.NoNewData -> {
//                            itemState = itemState.copy(
//                                loadState = SingleLoadState.Loaded
//                            )
//                        }
//
//                        is StoreReadResponse.Error.Exception -> {
//                            itemState = itemState.copy(
//                                loadState = SingleLoadState.Error(
//                                    errorFactory.create(
//                                        storeReadResponse.error
//                                    )
//                                )
//                            )
//                        }
//
//                        is StoreReadResponse.Error.Message -> {
//                            itemState = itemState.copy(
//                                loadState = SingleLoadState.Error(
//                                    errorFactory.create(
//                                        storeReadResponse.message
//                                    )
//                                )
//                            )
//                        }
//
//                        is StoreReadResponse.Error.Custom<*> -> {
//                            itemState = itemState.copy(
//                                loadState = SingleLoadState.Error(
//                                    error = storeReadResponse.error as E
//                                )
//                            )
//                        }
//
//                        StoreReadResponse.Initial -> {
//                            itemState = itemState.copy(
//                                loadState = SingleLoadState.Initial
//                            )
//                        }
//                    }
//                }
//            }
//        }
//
//        return itemState
//
//    }
//}
//
