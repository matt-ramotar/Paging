package org.mobilenativefoundation.storex.paging.custom


/**
 * A functional interface representing an effect to be launched.
 * This interface is typically used to define side effects or initialization
 * tasks that should be executed when the pager is created.
 */
fun interface LaunchEffect {
    /**
     * Invokes the launch effect.
     */
    operator fun invoke()
}