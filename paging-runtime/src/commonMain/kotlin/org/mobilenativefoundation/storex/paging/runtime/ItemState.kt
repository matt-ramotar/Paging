package org.mobilenativefoundation.storex.paging.runtime

data class ItemState<ItemValue : Any>(
    val item: ItemValue?,
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
        fun <ItemValue : Any> initial() =
            ItemState<ItemValue>(null, SingleLoadState.Initial)

        fun <ItemValue : Any> loaded(
            item: ItemValue,
            version: Long
        ) = ItemState<ItemValue>(item, SingleLoadState.Loaded, version)

        fun <ItemValue : Any> loading(
            item: ItemValue?,
            version: Long
        ) = ItemState<ItemValue>(item, SingleLoadState.Loading, version)

        fun <ItemValue : Any> errorRefreshing(
            item: ItemValue?,
            error: Throwable,
            version: Long
        ) = ItemState(
            item,
            SingleLoadState.Error.Exception(error, SingleLoadState.Error.Context.Refresh),
            version
        )

        fun <ItemValue : Any> cleared(
        ) = ItemState<ItemValue>(null, SingleLoadState.Cleared)
    }

}
