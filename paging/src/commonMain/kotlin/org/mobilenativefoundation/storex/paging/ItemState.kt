package org.mobilenativefoundation.storex.paging

import kotlinx.serialization.json.JsonObject

data class ItemState<Id : Identifier<*>, V : Identifiable<Id>>(
    val item: V?,
    val loadState: SingleLoadState,
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
        fun <Id : Identifier<*>, V : Identifiable<Id>> initial() =
            ItemState<Id, V>(null, SingleLoadState.Initial)

        fun <Id : Identifier<*>, V : Identifiable<Id>> loaded(
            item: V,
            version: Long
        ) =
            ItemState<Id, V>(item, SingleLoadState.Loaded, version)

        fun <Id : Identifier<*>, V : Identifiable<Id>> loading(
            item: V?,
            version: Long
        ) =
            ItemState<Id, V>(item, SingleLoadState.Loading, version)

        fun <Id : Identifier<*>, V : Identifiable<Id>> errorRefreshing(
            item: V?,
            error: Throwable,
            version: Long,
            extras: JsonObject? = null
        ) =
            ItemState<Id, V>(item, SingleLoadState.Error.Refreshing(error, extras), version)

        fun <Id : Identifier<*>, V : Identifiable<Id>> cleared(
        ) = ItemState<Id, V>(null, SingleLoadState.Cleared)
    }

}
