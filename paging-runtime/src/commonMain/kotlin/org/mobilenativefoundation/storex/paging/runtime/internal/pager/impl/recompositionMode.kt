package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import org.mobilenativefoundation.storex.paging.runtime.RecompositionMode
import app.cash.molecule.RecompositionMode as CashRecompositionMode

internal fun RecompositionMode.toCash(): CashRecompositionMode {
    return when (this) {
        RecompositionMode.ContextClock -> CashRecompositionMode.ContextClock
        RecompositionMode.Immediate -> CashRecompositionMode.Immediate
    }
}