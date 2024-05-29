package org.mobilenativefoundation.storex.paging.internal.impl

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun Throwable.extras(): JsonObject = JsonObject(
    mapOf(
        "message" to JsonPrimitive(message.orEmpty()),
        "stackTrace" to JsonPrimitive(stackTraceToString()),
        "cause" to JsonPrimitive(cause?.message.orEmpty())
    )

)