package org.mobilenativefoundation.storex.paging.internal.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class StateHolder<T : Any>(initialState: T) {
    private val _state = MutableStateFlow<T>(initialState)

    val state: StateFlow<T> = _state.asStateFlow()
    fun update(reducer: (prevState: T) -> T) {
        _state.value = reducer(_state.value)
    }

    fun update(nextState: T) {
        _state.value = nextState
    }
}