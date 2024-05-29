package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.serialization.json.JsonObject

data class PagingError(
    val encodedError: String,
    val extras: JsonObject? = null
) : Throwable()