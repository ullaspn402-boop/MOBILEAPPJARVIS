package com.aistudio.jarvis.voiceagent.data.backend

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class BackendTelemetry(
    val totalRequests: Long = 0,
    val localBypassCount: Long = 0,
    val cacheHitCount: Long = 0,
    val aiProviderCalls: Long = 0,
    val rateLimit429Events: Long = 0,
    val rateLimitBlocks: Long = 0,
    val averageLatencyMs: Long = 0,
    val totalInputTokens: Long = 0,
    val totalOutputTokens: Long = 0,
    val activeConcurrency: Int = 0,
    val activeQueueSize: Int = 0
)

/**
 * Real-time Usage Monitoring and Analytics Telemetry Engine.
 */
class UllasUsageMonitor {

    private val totalRequests = AtomicLong(0)
    private val localBypassCount = AtomicLong(0)
    private val cacheHitCount = AtomicLong(0)
    private val aiProviderCalls = AtomicLong(0)
    private val rateLimit429Events = AtomicLong(0)
    private val rateLimitBlocks = AtomicLong(0)
    private val totalLatencyMs = AtomicLong(0)
    private val latencySampleCount = AtomicLong(0)
    private val totalInputTokens = AtomicLong(0)
    private val totalOutputTokens = AtomicLong(0)

    private val userRequestCounts = ConcurrentHashMap<String, AtomicLong>()

    fun recordLocalBypass() {
        totalRequests.incrementAndGet()
        localBypassCount.incrementAndGet()
    }

    fun recordCacheHit(userId: String) {
        totalRequests.incrementAndGet()
        cacheHitCount.incrementAndGet()
        userRequestCounts.computeIfAbsent(userId) { AtomicLong(0) }.incrementAndGet()
    }

    fun recordAiCall(
        userId: String,
        latencyMs: Long,
        inputTokens: Int,
        outputTokens: Int
    ) {
        totalRequests.incrementAndGet()
        aiProviderCalls.incrementAndGet()
        totalLatencyMs.addAndGet(latencyMs)
        latencySampleCount.incrementAndGet()
        totalInputTokens.addAndGet(inputTokens.toLong())
        totalOutputTokens.addAndGet(outputTokens.toLong())
        userRequestCounts.computeIfAbsent(userId) { AtomicLong(0) }.incrementAndGet()
    }

    fun record429BackoffEvent() {
        rateLimit429Events.incrementAndGet()
    }

    fun recordRateLimitBlock() {
        totalRequests.incrementAndGet()
        rateLimitBlocks.incrementAndGet()
    }

    fun getTelemetry(activeConcurrency: Int = 0, activeQueueSize: Int = 0): BackendTelemetry {
        val samples = latencySampleCount.get()
        val avgLatency = if (samples > 0) totalLatencyMs.get() / samples else 0L

        return BackendTelemetry(
            totalRequests = totalRequests.get(),
            localBypassCount = localBypassCount.get(),
            cacheHitCount = cacheHitCount.get(),
            aiProviderCalls = aiProviderCalls.get(),
            rateLimit429Events = rateLimit429Events.get(),
            rateLimitBlocks = rateLimitBlocks.get(),
            averageLatencyMs = avgLatency,
            totalInputTokens = totalInputTokens.get(),
            totalOutputTokens = totalOutputTokens.get(),
            activeConcurrency = activeConcurrency,
            activeQueueSize = activeQueueSize
        )
    }

    fun getUserRequestCount(userId: String): Long {
        return userRequestCounts[userId]?.get() ?: 0L
    }

    fun resetMetrics() {
        totalRequests.set(0)
        localBypassCount.set(0)
        cacheHitCount.set(0)
        aiProviderCalls.set(0)
        rateLimit429Events.set(0)
        rateLimitBlocks.set(0)
        totalLatencyMs.set(0)
        latencySampleCount.set(0)
        totalInputTokens.set(0)
        totalOutputTokens.set(0)
        userRequestCounts.clear()
    }
}
