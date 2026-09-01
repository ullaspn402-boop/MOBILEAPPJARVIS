package com.aistudio.jarvis.voiceagent.data.backend

import com.aistudio.jarvis.voiceagent.model.ExecutionPlan
import com.aistudio.jarvis.voiceagent.model.PlanStep
import com.aistudio.jarvis.voiceagent.tools.RiskLevel
import java.util.UUID

/**
 * Alternative AI Provider example (Anthropic Claude 3.5 Sonnet).
 * Demonstrates how ULLAS architecture allows seamless provider swapping without modifying Android tools.
 */
class AnthropicClaudeProvider : AiProvider {
    override val providerId: String = "claude_3_5_sonnet"
    override val providerDisplayName: String = "Claude 3.5 Sonnet (Modular Adapter)"

    override suspend fun generateExecutionPlan(
        query: String,
        contextMemory: Map<String, String>,
        recentHistorySummary: String
    ): ExecutionPlanResult {
        // Fallback simulation / pluggable adapter
        val plan = ExecutionPlan(
            originalQuery = query,
            steps = listOf(
                PlanStep(
                    id = "step_${UUID.randomUUID().toString().take(8)}",
                    toolId = "web_search",
                    actionName = "Claude Intelligent Search",
                    description = "Analyze query via Claude provider: $query",
                    params = mapOf("query" to query),
                    riskLevel = RiskLevel.LOW
                )
            ),
            finalSpokenSummary = "Executed query via Claude adapter: $query"
        )
        return ExecutionPlanResult(
            isSuccess = true,
            plan = plan,
            providerName = providerDisplayName,
            latencyMs = 240,
            inputTokensUsed = 120,
            outputTokensUsed = 45
        )
    }
}

/**
 * Local Deterministic AI Provider.
 * Ultra-fast local fallback if internet or all cloud providers are unreachable or rate-limited.
 */
class LocalDeterministicAiProvider : AiProvider {
    override val providerId: String = "local_heuristic"
    override val providerDisplayName: String = "ULLAS On-Device Neural Engine"

    override suspend fun generateExecutionPlan(
        query: String,
        contextMemory: Map<String, String>,
        recentHistorySummary: String
    ): ExecutionPlanResult {
        val plan = ExecutionPlan(
            originalQuery = query,
            steps = listOf(
                PlanStep(
                    id = "step_${UUID.randomUUID().toString().take(8)}",
                    toolId = "web_search",
                    actionName = "Local Web Search",
                    description = "Searching: $query",
                    params = mapOf("query" to query),
                    riskLevel = RiskLevel.LOW
                )
            ),
            finalSpokenSummary = "Searching for $query."
        )
        return ExecutionPlanResult(
            isSuccess = true,
            plan = plan,
            providerName = providerDisplayName,
            latencyMs = 15,
            inputTokensUsed = 0,
            outputTokensUsed = 0
        )
    }
}

/**
 * Modular AI Provider Registry & Router.
 */
class ModularAiProviderRegistry {
    private val providers = mutableMapOf<String, AiProvider>(
        "gemini_2_0_flash" to GeminiAiProvider(),
        "claude_3_5_sonnet" to AnthropicClaudeProvider(),
        "local_heuristic" to LocalDeterministicAiProvider()
    )

    var activeProviderId: String = "gemini_2_0_flash"

    fun getActiveProvider(): AiProvider {
        return providers[activeProviderId] ?: providers["gemini_2_0_flash"] ?: LocalDeterministicAiProvider()
    }

    fun getAllProviders(): List<AiProvider> = providers.values.toList()

    fun switchProvider(providerId: String): Boolean {
        if (providers.containsKey(providerId)) {
            activeProviderId = providerId
            return true
        }
        return false
    }

    fun registerCustomProvider(provider: AiProvider) {
        providers[provider.providerId] = provider
    }
}
