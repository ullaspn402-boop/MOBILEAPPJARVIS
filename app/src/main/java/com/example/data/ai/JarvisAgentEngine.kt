package com.aistudio.jarvis.voiceagent.data.ai

import android.util.Log
import com.aistudio.jarvis.voiceagent.data.backend.UllasBackendClient
import com.aistudio.jarvis.voiceagent.data.backend.UllasBackendGateway
import com.aistudio.jarvis.voiceagent.model.ExecutionPlan
import com.aistudio.jarvis.voiceagent.model.PlanStep
import com.aistudio.jarvis.voiceagent.tools.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ULLAS Production Agent Engine.
 *
 * Implements Tiered Intelligence Routing:
 * Tier 1: Local Deterministic Intent Router (< 1ms, 0 API tokens, offline capable)
 * Tier 2: ULLAS Cloud Gateway -> Auth -> Rate Limiter -> Semantic Cache -> Request Queue & Backoff -> Gemini API
 */
class JarvisAgentEngine(
    val backendGateway: UllasBackendGateway = UllasBackendGateway()
) {
    private val tag = "JarvisAgentEngine"
    val backendClient = UllasBackendClient(backendGateway)

    suspend fun planActions(
        query: String,
        userPreferredName: String = "Sir",
        contextMemory: Map<String, String> = emptyMap(),
        recentHistorySummary: String = ""
    ): ExecutionPlan = withContext(Dispatchers.Default) {
        val trimmed = query.trim()

        // 1. FAST LOCAL INTENT ROUTING (CRITICAL OPTIMIZATION)
        // Commands like Open YouTube, Open Maps, Set alarm, Start timer, Open settings, Call, Message, Notes
        // are processed on-device instantly without any cloud/Gemini call.
        val localPlan = LocalIntentRouter.tryRouteLocalIntent(trimmed, userPreferredName, contextMemory)
        if (localPlan != null) {
            backendGateway.usageMonitor.recordLocalBypass()
            return@withContext localPlan
        }

        // 2. CLOUD BACKEND ROUTING FOR COMPLEX/REASONING QUERIES
        try {
            val response = backendClient.queryUllasBackend(
                query = trimmed,
                contextMemory = contextMemory,
                recentHistorySummary = recentHistorySummary
            )

            if (response.isSuccess && response.plan != null && response.plan.steps.isNotEmpty()) {
                return@withContext response.plan
            } else if (response.isSuccess && response.plan != null) {
                return@withContext response.plan
            }
        } catch (e: Exception) {
            Log.e(tag, "Backend call exception: ${e.message}")
        }

        // 3. SAFE DEFAULT FALLBACK
        return@withContext ExecutionPlan(
            originalQuery = trimmed,
            steps = listOf(
                PlanStep(
                    id = "step_${UUID.randomUUID().toString().take(8)}",
                    toolId = "web_search",
                    actionName = "Web Search",
                    description = "Search web for: \"$trimmed\"",
                    params = mapOf("query" to trimmed),
                    riskLevel = RiskLevel.LOW
                )
            ),
            finalSpokenSummary = "Searching the web for $trimmed."
        )
    }
}
