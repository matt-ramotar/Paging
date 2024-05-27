package org.mobilenativefoundation.storex.paging

import org.mobilenativefoundation.store5.core.Identifiable

data class ItemState<Id : Comparable<Id>, V : Identifiable<Id>, E : Any>(
    val item: V?,
    val loadState: ItemLoadState<E>
) {
    companion object {
        fun <Id : Comparable<Id>, V : Identifiable<Id>, E : Any> initial() =
            ItemState<Id, V, E>(null, ItemLoadState.Initial)

        fun <Id : Comparable<Id>, V : Identifiable<Id>, E : Any> loaded(item: V) =
            ItemState<Id, V, E>(item, ItemLoadState.Loaded)

        fun <Id : Comparable<Id>, V : Identifiable<Id>, E : Any> loading(item: V?) =
            ItemState<Id, V, E>(item, ItemLoadState.Loading)
    }
}
