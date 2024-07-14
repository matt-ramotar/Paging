package org.mobilenativefoundation.storex.paging.runtime

sealed class SingleLoadState {

    data object Initial : SingleLoadState()

    data object Loading : SingleLoadState()

    data object Refreshing : SingleLoadState()

    data object Loaded : SingleLoadState()

    sealed class Error : SingleLoadState() {

        enum class Context {
            InitialLoad,
            Refresh
        }

        abstract val context: Context

        data class Message(
            val error: String,
            override val context: Context
        ) : Error()

        data class Exception(
            val error: Throwable,
            override val context: Context
        ) : Error()
    }

    data object Cleared : SingleLoadState()
}
