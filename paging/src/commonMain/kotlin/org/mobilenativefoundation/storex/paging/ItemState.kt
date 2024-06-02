package org.mobilenativefoundation.storex.paging

import kotlinx.serialization.json.JsonObject

data class ItemState<Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any>(
    val item: V?,
    val loadState: SingleLoadState<E>,
    // TODO(): When do we update version?
    val itemVersion: Long = 0,
) {

    fun isPlaceholder(): Boolean {
        return item == null &&
                (loadState == SingleLoadState.Initial ||
                        loadState == SingleLoadState.Loading ||
                        loadState is SingleLoadState.Refreshing)
    }

    companion object {
        fun <Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> initial() =
            ItemState<Id, Q, V, E>(null, SingleLoadState.Initial)

        fun <Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> loaded(
            item: V,
            version: Long
        ) =
            ItemState<Id, Q, V, E>(item, SingleLoadState.Loaded, version)

        fun <Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> loading(
            item: V?,
            version: Long
        ) =
            ItemState<Id, Q, V, E>(item, SingleLoadState.Loading, version)

        fun <Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> errorRefreshing(
            item: V?,
            error: E,
            version: Long,
            extras: JsonObject? = null
        ) =
            ItemState<Id, Q, V, E>(item, SingleLoadState.Error.Refreshing(error, extras), version)

        fun <Id : Comparable<Id>, Q : Quantifiable<Id>, V : Identifiable<Id, Q>, E : Any> cleared(
        ) = ItemState<Id, Q, V, E>(null, SingleLoadState.Cleared)
    }

}
