package org.mobilenativefoundation.storex.paging.runtime.internal.pager.api

import org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl.DefaultExponentialBackoff
import kotlin.time.Duration.Companion.minutes

/**
 * Defines the contract for implementing an exponential backoff algorithm.
 *
 * This interface provides methods for calculating and executing delays
 * between retry attempts, typically increasing the delay exponentially
 * with each attempt to reduce system load during error conditions.
 */
interface ExponentialBackoff {

    /**
     * Executes a given suspend function after applying the calculated delay.
     *
     * @param retryCount The current retry attempt number (0-based).
     * @param block The suspend function to be executed after the delay.
     */
    suspend fun execute(retryCount: Int, block: suspend () -> Unit)

    companion object {
        /**
         * Creates a default implementation of ExponentialBackoff.
         *
         * @param initialDelayMs The initial delay in milliseconds before the first retry.
         * @param maxDelayMs The maximum delay in milliseconds, capping the exponential growth.
         * @param multiplier The factor by which the delay increases with each retry.
         * @param jitterFactor The maximum proportion of the delay to be added or subtracted randomly.
         * @return An instance of ExponentialBackoff with the specified parameters.
         */
        fun default(
            initialDelayMs: Long = 100,
            maxDelayMs: Long = 1.minutes.inWholeMilliseconds,
            multiplier: Double = 2.0,
            jitterFactor: Double = 0.1
        ): ExponentialBackoff = DefaultExponentialBackoff(initialDelayMs, maxDelayMs, multiplier, jitterFactor)
    }
}