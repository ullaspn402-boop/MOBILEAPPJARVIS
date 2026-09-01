package com.aistudio.jarvis.voiceagent.data.backend

import com.aistudio.jarvis.voiceagent.model.ExecutionPlan

/**
 * Result returned from an AI provider execution.
 */
data class ExecutionPlanResult(
    val isSuccess: Boolean,
    val plan: ExecutionPlan? = null,
    val errorMessage: String? = null,
    val providerName: String,
    val latencyMs: Long = 0,
    val inputTokensUsed: Int = 0,
    val outputTokensUsed: Int = 0,
    val isCached: Boolean = false
)

/**
 * Modular AI Provider Interface.
 * Allows seamless switching or fallback between Gemini, Anthropic, OpenAI, or Local models
 * without rewriting any Android UI or Tool execution layer.
 */
interface AiProvider {
    val providerId: String
    val providerDisplayName: String

    suspend fun generateExecutionPlan(
        query: String,
        contextMemory: Map<String, String>,
        recentHistorySummary: String = ""
    ): ExecutionPlanResult
}
