package org.mobilenativefoundation.storex.paging.runtime.internal.store.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.storex.paging.runtime.Identifiable
import org.mobilenativefoundation.storex.paging.runtime.Identifier
import org.mobilenativefoundation.storex.paging.runtime.ItemState
import org.mobilenativefoundation.storex.paging.runtime.SingleLoadState
import org.mobilenativefoundation.storex.paging.runtime.UpdatingItem
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.FetchingStateHolder
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.ItemStore
import org.mobilenativefoundation.storex.paging.runtime.internal.store.api.UpdatingItemPresenter

/**
 * A thread-safe presenter for managing self-updating items.
 *
 * This class handles the presentation logic for items that can update themselves,
 * ensuring thread safety and optimizing for runtime performance.
 *
 * @param Id The type of the item identifier.
 * @param V The type of the item value.
 * @property itemStore The store for retrieving and updating items.
 * @property updater An optional updater for posting item updates to an external system.
 */
internal class RealUpdatingItemPresenter<Id : Identifier<Id>, K : Comparable<K>, V : Identifiable<Id>>(
    private val itemStore: ItemStore<Id, V>,
    private val updater: Updater<Id, V, *>?,
    private val fetchingStateHolder: FetchingStateHolder<Id, K>,
) : UpdatingItemPresenter<Id, V> {

    @Composable
    override fun present(id: Id, events: Flow<UpdatingItem.Event<Id, V>>): ItemState<Id, V> {
        var itemState by remember {
            mutableStateOf(
                ItemState<Id, V>(
                    null,
                    SingleLoadState.Initial,
                    0
                )
            )
        }

        val itemValue by itemStore.observeItem(id).collectAsState(null)

        val currentEvent by events.collectAsState(initial = UpdatingItem.Event.Init)

        LaunchedEffect(id) {
            updateFetchingState(id)
        }

        LaunchedEffect(id, itemValue, currentEvent) {
            val nextState = reduceState(id, itemState, itemValue, currentEvent)

            // Update the state only if it has actually changed
            if (nextState != itemState) {
                itemState = nextState
            }
        }

        return itemState
    }

    private suspend fun reduceState(
        id: Id,
        prevState: ItemState<Id, V>,
        itemValue: V?,
        event: UpdatingItem.Event<out Id, out V>
    ): ItemState<Id, V> {
        val newState = when (event) {
            is UpdatingItem.Event.Clear -> handleClear(id)
            is UpdatingItem.Event.Refresh -> handleRefresh(id, prevState)
            is UpdatingItem.Event.Update -> handleUpdate(id, event.value, prevState)
            UpdatingItem.Event.Init -> handleItemValueChange(itemValue, prevState)
        }

        return if (newState.item != prevState.item || itemValue != prevState.item) {
            newState.copy(
                item = itemValue ?: newState.item,
                itemVersion = prevState.itemVersion + 1
            )
        } else {
            newState
        }
    }

    private fun handleItemValueChange(
        itemValue: V?,
        prevState: ItemState<Id, V>
    ): ItemState<Id, V> {
        return if (itemValue != prevState.item) {
            prevState.copy(
                item = itemValue,
                loadState = if (itemValue != null) SingleLoadState.Loaded else prevState.loadState
            )
        } else {
            prevState
        }
    }

    /**
     * Handles the Clear event for an item.
     *
     * @param id The identifier of the item to clear.
     * @return The updated ItemState after clearing.
     */
    private suspend fun handleClear(id: Id): ItemState<Id, V> {
        itemStore.removeItem(id)

        return ItemState(item = null, loadState = SingleLoadState.Cleared, itemVersion = 0)
    }

    /**
     * Handles the Refresh event for an item.
     *
     * @param id The identifier of the item to refresh.
     * @return The updated ItemState after refreshing.
     */
    private suspend fun handleRefresh(id: Id, prevState: ItemState<Id, V>): ItemState<Id, V> {
        val item = itemStore.getItem(id)
        return if (item != null) {
            ItemState(
                item = item,
                loadState = SingleLoadState.Loaded,
                itemVersion = prevState.itemVersion + 1
            )
        } else {
            ItemState(
                item = null, loadState = SingleLoadState.Error.Message(
                    error = "Not found",
                    context = SingleLoadState.Error.Context.Refresh
                ), itemVersion = 0
            )
        }
    }

    /**
     * Handles the Update event for an item.
     *
     * @param id The identifier of the item to update.
     * @return The updated ItemState after applying the update.
     */
    private suspend fun handleUpdate(
        id: Id,
        updatedItem: V,
        prevState: ItemState<Id, V>
    ): ItemState<Id, V> {
        itemStore.saveItem(updatedItem)
        updater?.post(id, updatedItem)
        val version = prevState.itemVersion + 1
        return ItemState(
            item = updatedItem,
            loadState = SingleLoadState.Loaded,
            itemVersion = version
        )
    }

    private suspend fun updateFetchingState(id: Id) {
        fetchingStateHolder.updateMaxItemAccessedSoFar(id)
        fetchingStateHolder.updateMinItemAccessedSoFar(id)
    }
}
