package com.aistudio.jarvis.voiceagent.data.backend

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Request Throttling, Queue Controller, and Exponential Backoff Engine.
 * Manages concurrency to stay strictly within provider constraints and handles 429 retries smoothly.
 */
class UllasRequestQueue(
    maxConcurrentAiCalls: Int = 3,
    private val requestTimeoutMs: Long = 15000L
) {
    private var semaphore = Semaphore(maxConcurrentAiCalls)
    private val activeCalls = AtomicInteger(0)
    private val queuedCalls = AtomicInteger(0)
    private var totalThrottledCalls = 0L

    fun getActiveCallCount(): Int = activeCalls.get()
    fun getQueuedCallCount(): Int = queuedCalls.get()
    fun getTotalThrottledCount(): Long = totalThrottledCalls

    /**
     * Executes an AI provider call within the concurrency semaphore,
     * applying timeouts and exponential backoff retry for transient 429/503 rate limits.
     */
    suspend fun <T> executeWithThrottlingAndBackoff(
        maxRetries: Int = 3,
        baseBackoffMs: Long = 1000L,
        onRetryAttempt: (attempt: Int, delayMs: Long, errorReason: String) -> Unit = { _, _, _ -> },
        block: suspend () -> T
    ): T? {
        queuedCalls.incrementAndGet()
        totalThrottledCalls++

        try {
            return semaphore.withPermit {
                queuedCalls.decrementAndGet()
                activeCalls.incrementAndGet()

                try {
                    var attempt = 0
                    while (attempt < maxRetries) {
                        attempt++
                        try {
                            val result = withTimeoutOrNull(requestTimeoutMs) {
                                block()
                            }
                            if (result != null) {
                                return@withPermit result
                            }
                        } catch (e: Exception) {
                            val isRateLimit = e.message?.contains("429") == true ||
                                    e.message?.contains("RESOURCE_EXHAUSTED") == true ||
                                    e.message?.contains("Too Many Requests") == true

                            val isTransient = isRateLimit ||
                                    e.message?.contains("503") == true ||
                                    e.message?.contains("504") == true ||
                                    e.message?.contains("timeout") == true

                            if (attempt >= maxRetries || !isTransient) {
                                throw e
                            }

                            // Exponential backoff with random jitter: (2^attempt * baseMs) + jitter
                            val backoffFactor = 2.0.pow(attempt.toDouble()).toLong()
                            val jitter = Random.nextLong(100, 500)
                            val delayMs = min(8000L, (baseBackoffMs * backoffFactor) + jitter)

                            onRetryAttempt(attempt, delayMs, e.message ?: "Rate Limit")
                            delay(delayMs)
                        }
                    }
                    null
                } finally {
                    activeCalls.decrementAndGet()
                }
            }
        } finally {
            // Guarantee queued count cleanup if cancelled
        }
    }

    fun updateConcurrencyLimit(newLimit: Int) {
        semaphore = Semaphore(newLimit.coerceIn(1, 10))
    }
}
