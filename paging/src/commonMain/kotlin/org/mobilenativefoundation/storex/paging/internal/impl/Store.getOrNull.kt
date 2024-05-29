package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse


suspend fun <Key : Any, Output : Any> Store<Key, Output>.getOrNull(key: Key): Output? =
    stream(
        StoreReadRequest.cached(
            key,
            refresh = false
        )
    ).filterIsInstance<StoreReadResponse.Data<Output>>().firstOrNull()?.requireData()