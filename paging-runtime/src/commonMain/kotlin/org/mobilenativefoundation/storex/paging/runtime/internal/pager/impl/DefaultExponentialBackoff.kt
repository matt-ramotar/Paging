package org.mobilenativefoundation.storex.paging.runtime.internal.pager.impl

import kotlinx.coroutines.delay
import org.mobilenativefoundation.storex.paging.runtime.internal.pager.api.ExponentialBackoff
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Default implementation of the [ExponentialBackoff] interface.
 *
 * This class implements an exponential backoff algorithm with jitter for retry mechanisms.
 * It calculates delays between retry attempts, increasing the delay exponentially with each
 * attempt and adding a random jitter to prevent synchronized retries from multiple clients.
 *
 * @property initialDelayMs The initial delay in milliseconds before the first retry.
 * @property maxDelayMs The maximum delay in milliseconds, capping the exponential growth.
 * @property multiplier The factor by which the delay increases with each retry.
 * @property jitterFactor The maximum proportion of the delay to be added or subtracted randomly.
 */
class DefaultExponentialBackoff(
    private val initialDelayMs: Long,
    private val maxDelayMs: Long,
    private val multiplier: Double,
    private val jitterFactor: Double
) : ExponentialBackoff {
    /**
     * Executes a given suspend function after applying the calculated delay.
     *
     * @param retryCount The current retry attempt number.
     * @param block The suspend function to be executed after the delay.
     */
    override suspend fun execute(retryCount: Int, block: suspend () -> Unit) {
        val delayMs = calculateDelay(retryCount)
        delay(delayMs)
        block()
    }

    /**
     * Calculates the delay for a given retry attempt.
     *
     * The delay is calculated using the formula:
     * delay = min(initialDelay * (multiplier ^ retryCount), maxDelay) + jitter
     *
     * @param retryCount The current retry attempt number.
     * @return The calculated delay in milliseconds.
     */
    private fun calculateDelay(retryCount: Int): Long {
        // Calculate the base delay using exponential backoff
        val baseDelay = (initialDelayMs * multiplier.pow(retryCount.toDouble())).toLong()

        // Cap the delay at the maximum allowed delay
        val maxDelayBeforeJitter = min(baseDelay, maxDelayMs)

        // Calculate jitter as a random value between -jitterFactor and +jitterFactor of the delay
        val jitter = (Random.nextDouble() * 2 - 1) * jitterFactor * maxDelayBeforeJitter

        // Add jitter to the delay and ensure it's not negative
        return (maxDelayBeforeJitter + jitter).toLong().coerceAtLeast(0L)
    }

}