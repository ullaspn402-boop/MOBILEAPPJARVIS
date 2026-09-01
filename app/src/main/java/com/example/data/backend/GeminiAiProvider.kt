package com.aistudio.jarvis.voiceagent.data.backend

import android.util.Log
import com.aistudio.jarvis.voiceagent.BuildConfig
import com.aistudio.jarvis.voiceagent.data.ai.GeminiClient
import com.aistudio.jarvis.voiceagent.data.ai.GeminiContent
import com.aistudio.jarvis.voiceagent.data.ai.GeminiPart
import com.aistudio.jarvis.voiceagent.data.ai.GeminiRequest
import com.aistudio.jarvis.voiceagent.model.ExecutionPlan
import com.aistudio.jarvis.voiceagent.model.PlanStep
import com.aistudio.jarvis.voiceagent.tools.RiskLevel
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Server-side Gemini AI Provider Implementation.
 * Uses compact prompts, structured JSON schema parsing, and token tracking.
 * Never exposes the internal API key to the client.
 */
class GeminiAiProvider : AiProvider {

    override val providerId: String = "gemini_2_0_flash"
    override val providerDisplayName: String = "Gemini 2.0 Flash (On-Device AI)"

    private val tag = "GeminiAiProvider"

    override suspend fun generateExecutionPlan(
        query: String,
        contextMemory: Map<String, String>,
        recentHistorySummary: String
    ): ExecutionPlanResult {
        val startTime = System.currentTimeMillis()

        val serverSecretApiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (serverSecretApiKey.isBlank() || serverSecretApiKey.contains("MY_GEMINI_API_KEY")) {
            return ExecutionPlanResult(
                isSuccess = false,
                errorMessage = "Server Gemini API key is unconfigured on the backend.",
                providerName = providerDisplayName,
                latencyMs = System.currentTimeMillis() - startTime
            )
        }

        val compactSystemPrompt = UllasPromptMinimizer.buildCompactSystemPrompt()
        val minimizedMemory = UllasPromptMinimizer.minimizeMemoryContext(contextMemory)
        val minimizedQuery = UllasPromptMinimizer.minimizeQuery(query)

        val promptContent = StringBuilder()
        if (minimizedMemory.isNotBlank()) {
            promptContent.append("Memory: ").append(minimizedMemory).append("\n")
        }
        if (recentHistorySummary.isNotBlank()) {
            promptContent.append("Recent: ").append(recentHistorySummary.take(150)).append("\n")
        }
        promptContent.append("Command: ").append(minimizedQuery)

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = promptContent.toString()))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = compactSystemPrompt))
            )
        )

        try {
            val response = GeminiClient.service.generateContent(serverSecretApiKey, request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (rawJson.isNullOrBlank()) {
                return ExecutionPlanResult(
                    isSuccess = false,
                    errorMessage = "Empty response from AI engine.",
                    providerName = providerDisplayName,
                    latencyMs = System.currentTimeMillis() - startTime
                )
            }

            val cleanJson = rawJson.replace("```json", "").replace("```", "").trim()
            val jsonObj = JSONObject(cleanJson)
            val summary = jsonObj.optString("summary", "Executing planned actions.")
            val stepsArr = jsonObj.optJSONArray("steps") ?: JSONArray()

            val stepsList = mutableListOf<PlanStep>()
            var requiresConfirmation = false
            var confirmationStep: PlanStep? = null

            for (i in 0 until stepsArr.length()) {
                val stepObj = stepsArr.getJSONObject(i)
                val toolId = stepObj.getString("toolId")
                val actionName = stepObj.optString("actionName", "Action ${i + 1}")
                val description = stepObj.optString("description", "")
                val riskLevelStr = stepObj.optString("riskLevel", "LOW")
                val riskLevel = try { RiskLevel.valueOf(riskLevelStr) } catch (e: Exception) { RiskLevel.LOW }

                val paramsObj = stepObj.optJSONObject("params") ?: JSONObject()
                val paramsMap = mutableMapOf<String, Any?>()
                val keys = paramsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    paramsMap[k] = paramsObj.get(k)
                }

                val planStep = PlanStep(
                    id = "step_${UUID.randomUUID().toString().take(8)}",
                    toolId = toolId,
                    actionName = actionName,
                    description = description,
                    params = paramsMap,
                    riskLevel = riskLevel
                )
                stepsList.add(planStep)

                if (riskLevel == RiskLevel.HIGH && confirmationStep == null) {
                    requiresConfirmation = true
                    confirmationStep = planStep
                }
            }

            val plan = ExecutionPlan(
                originalQuery = query,
                steps = stepsList,
                finalSpokenSummary = summary,
                isMultiStep = stepsList.size > 1,
                requiresConfirmation = requiresConfirmation,
                confirmationStep = confirmationStep
            )

            val latency = System.currentTimeMillis() - startTime
            val approxInputTokens = (compactSystemPrompt.length + promptContent.length) / 4
            val approxOutputTokens = cleanJson.length / 4

            return ExecutionPlanResult(
                isSuccess = true,
                plan = plan,
                providerName = providerDisplayName,
                latencyMs = latency,
                inputTokensUsed = approxInputTokens,
                outputTokensUsed = approxOutputTokens
            )

        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown upstream error"
            Log.e(tag, "Gemini provider upstream error: $errorMsg")
            // Throw so request queue can apply exponential backoff if 429
            throw e
        }
    }
}
