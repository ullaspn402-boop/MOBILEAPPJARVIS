package com.aistudio.jarvis.voiceagent.data.backend

import android.util.Log
import com.aistudio.jarvis.voiceagent.model.ExecutionPlan

data class UllasBackendRequest(
    val authToken: String,
    val query: String,
    val contextMemory: Map<String, String> = emptyMap(),
    val recentHistorySummary: String = "",
    val clientVersion: String = "3.5.0-prod"
)

data class UllasBackendResponse(
    val statusCode: Int, // 200 = OK, 401 = Auth Error, 429 = Rate Limited, 500 = Sanitized Server Error
    val isSuccess: Boolean,
    val plan: ExecutionPlan? = null,
    val message: String? = null,
    val providerUsed: String = "ULLAS Cloud Gateway",
    val latencyMs: Long = 0,
    val isCached: Boolean = false,
    val remainingDailyQuota: Int = 0,
    val remainingUserRpm: Int = 0,
    val retryAfterSeconds: Long = 0
)

/**
 * ULLAS Production Backend API Server Gateway.
 * Sits securely between the Android Application and the Gemini / Cloud AI Providers.
 *
 * Architecture Flow:
 * Android Client -> ULLAS Backend Gateway -> Auth & Rate Limiter -> Request Queue -> Active AI Provider -> Cache -> Android Client.
 */
class UllasBackendGateway {

    private val tag = "UllasBackendGateway"

    val authManager = UllasAuthManager()
    val adminConfig = UllasAdminConfigManager()
    val rateLimiter = UllasRateLimiter(globalMaxRpm = adminConfig.config.globalMaxRpm)
    val requestQueue = UllasRequestQueue(maxConcurrentAiCalls = adminConfig.config.maxConcurrency)
    val semanticCache = UllasSemanticCache(defaultTtlMillis = adminConfig.config.cacheTtlMinutes * 60 * 1000L)
    val providerRegistry = ModularAiProviderRegistry()
    val usageMonitor = UllasUsageMonitor()

    suspend fun processRequest(request: UllasBackendRequest): UllasBackendResponse {
        val startTime = System.currentTimeMillis()

        // 1. Per-User Authentication & Abuse Shield
        val authResult = authManager.validateToken(request.authToken)
        if (!authResult.isValid || authResult.userSession == null) {
            usageMonitor.recordRateLimitBlock()
            return UllasBackendResponse(
                statusCode = 401,
                isSuccess = false,
                message = authResult.errorMessage ?: "Authentication failure.",
                latencyMs = System.currentTimeMillis() - startTime
            )
        }
        val userSession = authResult.userSession

        // 2. Server-side Rate Limiting & Daily/Monthly Quotas
        val rateLimitDecision = rateLimiter.checkRateLimit(userSession)
        if (!rateLimitDecision.isAllowed) {
            usageMonitor.recordRateLimitBlock()
            // Seamless On-Device Fallback: process query locally without failing
            val fallbackPlan = LocalDeterministicAiProvider().generateExecutionPlan(request.query, request.contextMemory)
            return UllasBackendResponse(
                statusCode = 200,
                isSuccess = true,
                plan = fallbackPlan.plan,
                providerUsed = "ULLAS On-Device Engine (Quota Saver)",
                message = "Rate limit reached — processed on-device.",
                remainingDailyQuota = rateLimitDecision.remainingDailyQuota,
                remainingUserRpm = rateLimitDecision.remainingUserRpm,
                latencyMs = System.currentTimeMillis() - startTime
            )
        }

        // 3. High-Performance Semantic Cache check
        val cachedPlan = semanticCache.get(request.query)
        if (cachedPlan != null) {
            usageMonitor.recordCacheHit(userSession.userId)
            return UllasBackendResponse(
                statusCode = 200,
                isSuccess = true,
                plan = cachedPlan,
                providerUsed = "ULLAS Semantic Cache (0ms AI Latency)",
                isCached = true,
                latencyMs = System.currentTimeMillis() - startTime,
                remainingDailyQuota = rateLimitDecision.remainingDailyQuota,
                remainingUserRpm = rateLimitDecision.remainingUserRpm
            )
        }

        // 4. Request Throttling, Priority Queue, and Exponential Backoff for AI Provider
        val activeProvider = providerRegistry.getActiveProvider()

        try {
            val planResult = requestQueue.executeWithThrottlingAndBackoff(
                maxRetries = 3,
                baseBackoffMs = 1000L,
                onRetryAttempt = { attempt, delayMs, reason ->
                    usageMonitor.record429BackoffEvent()
                    Log.w(tag, "Upstream AI rate limited (Attempt $attempt). Backing off for ${delayMs}ms. Reason: $reason")
                }
            ) {
                activeProvider.generateExecutionPlan(
                    query = request.query,
                    contextMemory = request.contextMemory,
                    recentHistorySummary = request.recentHistorySummary
                )
            }

            if (planResult != null && planResult.isSuccess && planResult.plan != null) {
                // Cache successful plan
                semanticCache.put(request.query, planResult.plan)

                // Record usage metrics
                usageMonitor.recordAiCall(
                    userId = userSession.userId,
                    latencyMs = planResult.latencyMs,
                    inputTokens = planResult.inputTokensUsed,
                    outputTokens = planResult.outputTokensUsed
                )

                return UllasBackendResponse(
                    statusCode = 200,
                    isSuccess = true,
                    plan = planResult.plan,
                    providerUsed = planResult.providerName,
                    latencyMs = System.currentTimeMillis() - startTime,
                    isCached = false,
                    remainingDailyQuota = rateLimitDecision.remainingDailyQuota,
                    remainingUserRpm = rateLimitDecision.remainingUserRpm
                )
            } else {
                // Graceful fallback to local heuristic provider if upstream returned failure
                Log.w(tag, "Upstream AI provider did not return valid plan. Activating resilient fallback.")
                val fallbackPlan = providerRegistry.getActiveProvider().let {
                    LocalDeterministicAiProvider().generateExecutionPlan(request.query, request.contextMemory)
                }

                return UllasBackendResponse(
                    statusCode = 200,
                    isSuccess = true,
                    plan = fallbackPlan.plan,
                    providerUsed = "ULLAS Resilience Engine (Fallback)",
                    latencyMs = System.currentTimeMillis() - startTime,
                    remainingDailyQuota = rateLimitDecision.remainingDailyQuota,
                    remainingUserRpm = rateLimitDecision.remainingUserRpm
                )
            }

        } catch (e: Exception) {
            Log.e(tag, "Server error in backend gateway: ${e.message}")
            // Sanitized user error - never expose internal stack trace or API credentials
            val fallbackPlan = LocalDeterministicAiProvider().generateExecutionPlan(request.query, request.contextMemory)
            return UllasBackendResponse(
                statusCode = 200,
                isSuccess = true,
                plan = fallbackPlan.plan,
                providerUsed = "ULLAS Resilience Engine (Emergency Safe-Mode)",
                message = "Traffic optimized via on-device intelligence.",
                latencyMs = System.currentTimeMillis() - startTime,
                remainingDailyQuota = rateLimitDecision.remainingDailyQuota,
                remainingUserRpm = rateLimitDecision.remainingUserRpm
            )
        }
    }
}
